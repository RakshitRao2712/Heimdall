package com.scraper.leadscraper.teams;

import com.scraper.leadscraper.auth.ProfileStore;
import com.scraper.leadscraper.history.LeadFiles;
import com.scraper.leadscraper.history.SearchHistory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

/** Durable, single-instance team store. Every read and write checks current membership. */
@Service
public class TeamStore {
    public record Actor(String email, String name) { }
    public record Member(Actor user, String joinedAt) { }
    public record Note(String id, String text, Actor author, String createdAt) { }
    public record Lead(String id, SearchHistory.Lead data, boolean done, Actor updatedBy, String updatedAt, long version, List<Note> notes) { }
    public record Folder(String id, String name, String sourceFileId, String method, String query, Actor importedBy, String importedAt, List<Lead> leads) { }
    public record Team(String id, String name, String leaderEmail, String inviteCode, String createdAt, long revision, List<Member> members, List<Folder> folders) { }
    public record Summary(String id, String name, String role, int members, int folders, int leads, int done, String createdAt) { }
    public record View(String id, String name, String leaderEmail, String inviteCode, String createdAt, long revision, List<Member> members, List<Folder> folders) { }
    private final Path directory; private final ObjectMapper mapper; private final LeadFiles files; private final ProfileStore profiles;
    private final SecureRandom random = new SecureRandom();
    private record Attempts(long since, int count) { }
    private final Map<String, Attempts> joinAttempts = new HashMap<>();
    public TeamStore(@Value("${heimdall.teams.directory:.data/teams}") String directory, ObjectMapper mapper, LeadFiles files, ProfileStore profiles) {
        this.directory = Path.of(directory); this.mapper = mapper; this.files = files; this.profiles = profiles;
    }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Team or shared item not found, or access is no longer available."); }
    private String id(String value) {
        if (value == null || !value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) throw missing();
        return value;
    }
    private boolean contains(Team team, String email) { return team.members.stream().anyMatch(member -> member.user.email.equals(email)); }
    private Team read(String teamId) {
        Path file = directory.resolve(id(teamId) + ".json"); if (!Files.exists(file)) throw missing();
        try { return mapper.readValue(Files.readAllBytes(file), Team.class); } catch (Exception e) { throw new IllegalStateException("Cannot load this team", e); }
    }
    private List<Team> all() {
        if (!Files.exists(directory)) return List.of();
        try (var paths = Files.list(directory)) {
            return paths.filter(path -> path.getFileName().toString().matches("[0-9a-f-]{36}\\.json")).map(path -> read(path.getFileName().toString().replace(".json", ""))).toList();
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot load teams", e); }
    }
    private Team require(String teamId, String email) { Team team = read(teamId); if (!contains(team, email)) throw missing(); return team; }
    private void leader(Team team, String email) { if (!team.leaderEmail.equals(email)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the team leader can manage membership and join codes."); }
    private void write(Team team) {
        Path temporary = null;
        try {
            Files.createDirectories(directory); temporary = Files.createTempFile(directory, "team-", ".tmp");
            try { Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-------")); } catch (UnsupportedOperationException ignored) { }
            Files.write(temporary, mapper.writeValueAsBytes(team));
            Files.move(temporary, directory.resolve(team.id + ".json"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) { throw new IllegalStateException("Cannot save team changes", e); }
        finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (Exception ignored) { } }
    }
    private Actor actor(String email) { var profile = profiles.get(email); return new Actor(email, profile.username().isBlank() ? email.split("@")[0] : profile.username()); }
    private String code() { byte[] bytes = new byte[24]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String clean(String input, String label, int max) {
        String value = input == null ? "" : input.strip();
        if (value.isEmpty() || value.length() > max || value.chars().anyMatch(c -> Character.isISOControl(c) && (!label.equals("Note") || (c != '\n' && c != '\t')))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " must contain 1–" + max + " characters.");
        return value;
    }
    private Team next(Team old, List<Member> members, List<Folder> folders, String code) {
        return new Team(old.id, old.name, old.leaderEmail, code, old.createdAt, old.revision + 1, List.copyOf(members), List.copyOf(folders));
    }
    private View view(Team team, String email) { return new View(team.id, team.name, team.leaderEmail, team.leaderEmail.equals(email) ? team.inviteCode : null, team.createdAt, team.revision, team.members.stream().map(member -> new Member(actor(member.user.email), member.joinedAt)).toList(), team.folders); }
    public synchronized List<Summary> list(String email) {
        return all().stream().filter(team -> contains(team, email)).sorted(Comparator.comparing(Team::createdAt).reversed()).map(team -> {
            int leads = team.folders.stream().mapToInt(folder -> folder.leads.size()).sum();
            int done = team.folders.stream().flatMap(folder -> folder.leads.stream()).mapToInt(lead -> lead.done ? 1 : 0).sum();
            return new Summary(team.id, team.name, team.leaderEmail.equals(email) ? "leader" : "member", team.members.size(), team.folders.size(), leads, done, team.createdAt);
        }).toList();
    }
    public synchronized View get(String teamId, String email) { return view(require(teamId, email), email); }
    public synchronized View create(String email, String input) {
        if (list(email).size() >= 20) throw new ResponseStatusException(HttpStatus.CONFLICT, "You can belong to up to 20 teams.");
        String name = clean(input, "Team name", 80); String now = Instant.now().toString();
        Team team = new Team(UUID.randomUUID().toString(), name, email, code(), now, 1, List.of(new Member(actor(email), now)), List.of());
        write(team); return view(team, email);
    }
    public synchronized View join(String email, String input) {
        long now = System.currentTimeMillis(); joinAttempts.entrySet().removeIf(e -> now - e.getValue().since > 900_000);
        var attempt = joinAttempts.get(email);
        if (joinAttempts.size() >= 10_000 || (attempt != null && attempt.count >= 15)) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many join attempts. Try again in 15 minutes.");
        joinAttempts.put(email, new Attempts(attempt == null ? now : attempt.since, attempt == null ? 1 : attempt.count + 1));
        String token = input == null ? "" : input.strip();
        if (!token.matches("[A-Za-z0-9_-]{32}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid team join code.");
        Team team = all().stream().filter(item -> java.security.MessageDigest.isEqual(item.inviteCode.getBytes(java.nio.charset.StandardCharsets.UTF_8), token.getBytes(java.nio.charset.StandardCharsets.UTF_8))).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "This join code is invalid or has been replaced."));
        if (contains(team, email)) return view(team, email);
        if (team.members.size() >= 50 || list(email).size() >= 20) throw new ResponseStatusException(HttpStatus.CONFLICT, "Team membership limit reached (50 members per team, 20 teams per account).");
        var members = new ArrayList<>(team.members); members.add(new Member(actor(email), Instant.now().toString()));
        Team updated = next(team, members, team.folders, team.inviteCode); write(updated); return view(updated, email);
    }
    public synchronized View rotate(String teamId, String email) {
        Team team = require(teamId, email); leader(team, email);
        Team updated = next(team, team.members, team.folders, code()); write(updated); return view(updated, email);
    }
    public synchronized void removeMember(String teamId, String email, String memberEmail) {
        Team team = require(teamId, email); leader(team, email);
        if (email.equals(memberEmail)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The leader cannot remove themselves.");
        if (!contains(team, memberEmail)) throw missing();
        write(next(team, team.members.stream().filter(member -> !member.user.email.equals(memberEmail)).toList(), team.folders, team.inviteCode));
    }
    public synchronized void leave(String teamId, String email) {
        Team team = require(teamId, email);
        if (team.leaderEmail.equals(email)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The team leader must remain in the team.");
        write(next(team, team.members.stream().filter(member -> !member.user.email.equals(email)).toList(), team.folders, team.inviteCode));
    }
    public synchronized View importFile(String teamId, String email, String fileId) {
        Team team = require(teamId, email);
        var source = files.list(email).stream().filter(file -> file.id().equals(fileId)).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Choose a lead file from your own library."));
        if (team.folders.stream().anyMatch(folder -> folder.sourceFileId.equals(fileId) && folder.importedBy.email.equals(email))) throw new ResponseStatusException(HttpStatus.CONFLICT, "This file is already shared with this team.");
        int size = team.folders.stream().mapToInt(folder -> folder.leads.size()).sum();
        if (source.leads().isEmpty() || team.folders.size() >= 50 || size + source.leads().size() > 2000) throw new ResponseStatusException(HttpStatus.CONFLICT, "Use a non-empty file. Each team supports up to 50 folders and 2,000 leads.");
        String now = Instant.now().toString();
        var leads = source.leads().stream().map(lead -> new Lead(UUID.randomUUID().toString(), lead, false, null, null, 0, List.of())).toList();
        var folders = new ArrayList<>(team.folders); folders.add(0, new Folder(UUID.randomUUID().toString(), source.name(), source.id(), source.method(), source.query(), actor(email), now, leads));
        Team updated = next(team, team.members, folders, team.inviteCode); write(updated); return view(updated, email);
    }
    private interface Change { Lead apply(Lead old); }
    private View changeLead(String teamId, String email, String folderId, String leadId, Change change) {
        Team team = require(teamId, email); boolean found = false; var folders = new ArrayList<Folder>();
        for (Folder folder : team.folders) {
            if (!folder.id.equals(folderId)) { folders.add(folder); continue; }
            var leads = new ArrayList<Lead>();
            for (Lead lead : folder.leads) { if (lead.id.equals(leadId)) { found = true; leads.add(change.apply(lead)); } else leads.add(lead); }
            folders.add(new Folder(folder.id, folder.name, folder.sourceFileId, folder.method, folder.query, folder.importedBy, folder.importedAt, List.copyOf(leads)));
        }
        if (!found) throw missing();
        Team updated = next(team, team.members, folders, team.inviteCode); write(updated); return view(updated, email);
    }
    public synchronized View status(String teamId, String email, String folderId, String leadId, boolean done, long version) {
        return changeLead(teamId, email, folderId, leadId, old -> {
            if (old.version != version) throw new ResponseStatusException(HttpStatus.CONFLICT, "This lead changed. Refresh it before updating its status.");
            return new Lead(old.id, old.data, done, actor(email), Instant.now().toString(), old.version + 1, old.notes);
        });
    }
    public synchronized View note(String teamId, String email, String folderId, String leadId, String input) {
        String text = clean(input, "Note", 2000);
        return changeLead(teamId, email, folderId, leadId, old -> {
            if (old.notes.size() >= 100) throw new ResponseStatusException(HttpStatus.CONFLICT, "This lead already has 100 notes.");
            var notes = new ArrayList<>(old.notes); notes.add(new Note(UUID.randomUUID().toString(), text, actor(email), Instant.now().toString()));
            return new Lead(old.id, old.data, old.done, old.updatedBy, old.updatedAt, old.version + 1, List.copyOf(notes));
        });
    }
}
