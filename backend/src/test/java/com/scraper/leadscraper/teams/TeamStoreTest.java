package com.scraper.leadscraper.teams;

import com.scraper.leadscraper.auth.ProfileStore;
import com.scraper.leadscraper.history.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class TeamStoreTest {
    @TempDir Path directory;
    TeamStore teams; LeadFiles files; SearchHistory history; ProfileStore profiles;
    ObjectMapper mapper = new ObjectMapper();
    @BeforeEach void setup() {
        history = new SearchHistory(directory.resolve("history").toString(), mapper);
        files = new LeadFiles(directory.resolve("files").toString(), mapper, history);
        profiles = new ProfileStore(directory.resolve("profiles").toString(), mapper);
        profiles.save("leader@example.com", new ProfileStore.Profile("Leader", "", "Team Captain", "", true));
        profiles.save("member@example.com", new ProfileStore.Profile("Member", "", "Alex", "", true));
        teams = new TeamStore(directory.resolve("teams").toString(), mapper, files, profiles);
    }
    LeadFiles.File source(String email) {
        var lead = new SearchHistory.Lead("Sample business", "https://example.com", List.of("+1 202-555-0100"), "Sample address", "2024", null);
        var run = history.save(email, "copyright", "Sample query", 1000, List.of(lead));
        return files.create(email, "Prospects", run.id());
    }
    @Test void creatorLeadsAndNonMembersCannotReadOrWrite() {
        var team = teams.create("leader@example.com", " Growth team ");
        assertEquals("Growth team", team.name()); assertEquals("leader", teams.list("leader@example.com").getFirst().role());
        assertEquals(32, team.inviteCode().length()); assertEquals("Team Captain", team.members().getFirst().user().name());
        assertTrue(teams.list("outsider@example.com").isEmpty());
        assertThrows(ResponseStatusException.class, () -> teams.get(team.id(), "outsider@example.com"));
        assertThrows(ResponseStatusException.class, () -> teams.importFile(team.id(), "outsider@example.com", "anything"));
        assertThrows(ResponseStatusException.class, () -> teams.get("../escape", "leader@example.com"));
        var member = teams.join("member@example.com", team.inviteCode());
        assertNull(member.inviteCode()); assertEquals(2, teams.join("member@example.com", team.inviteCode()).members().size());
        assertThrows(ResponseStatusException.class, () -> teams.rotate(team.id(), "member@example.com"));
        assertThrows(ResponseStatusException.class, () -> teams.removeMember(team.id(), "member@example.com", "leader@example.com"));
        assertThrows(ResponseStatusException.class, () -> teams.leave(team.id(), "leader@example.com"));
    }
    @Test void membersShareTheirOwnFilesAndCollaborationPersistsWithAttribution() {
        var team = teams.create("leader@example.com", "Sales"); teams.join("member@example.com", team.inviteCode());
        var source = source("member@example.com");
        assertThrows(ResponseStatusException.class, () -> teams.importFile(team.id(), "leader@example.com", source.id()));
        var shared = teams.importFile(team.id(), "member@example.com", source.id());
        assertThrows(ResponseStatusException.class, () -> teams.importFile(team.id(), "member@example.com", source.id()));
        var folder = shared.folders().getFirst(); var lead = folder.leads().getFirst();
        var noted = teams.note(team.id(), "member@example.com", folder.id(), lead.id(), "  Called today. Follow up Friday.  ");
        var note = noted.folders().getFirst().leads().getFirst().notes().getFirst();
        assertEquals("Called today. Follow up Friday.", note.text()); assertEquals("Alex", note.author().name()); assertEquals("member@example.com", note.author().email());
        assertThrows(ResponseStatusException.class, () -> teams.status(team.id(), "leader@example.com", folder.id(), lead.id(), true, 0));
        var done = teams.status(team.id(), "leader@example.com", folder.id(), lead.id(), true, 1);
        assertTrue(done.folders().getFirst().leads().getFirst().done());
        assertEquals("leader@example.com", done.folders().getFirst().leads().getFirst().updatedBy().email());
        assertEquals(1, teams.list("member@example.com").getFirst().done());
        files.rename("member@example.com", source.id(), "Private rename");
        var reopened = new TeamStore(directory.resolve("teams").toString(), mapper, files, profiles).get(team.id(), "member@example.com");
        assertEquals("Prospects", reopened.folders().getFirst().name()); assertEquals(1, reopened.folders().getFirst().leads().getFirst().notes().size());
        assertTrue(reopened.folders().getFirst().leads().getFirst().done());
        assertThrows(ResponseStatusException.class, () -> teams.note(team.id(), "member@example.com", folder.id(), lead.id(), " "));
        assertThrows(ResponseStatusException.class, () -> teams.note(team.id(), "member@example.com", folder.id(), lead.id(), "x".repeat(2001)));
    }
    @Test void rotationRemovalAndLeavingRevokeAccess() {
        var team = teams.create("leader@example.com", "Sales"); teams.join("member@example.com", team.inviteCode());
        var rotated = teams.rotate(team.id(), "leader@example.com");
        assertNotEquals(team.inviteCode(), rotated.inviteCode());
        assertThrows(ResponseStatusException.class, () -> teams.join("new@example.com", team.inviteCode()));
        teams.removeMember(team.id(), "leader@example.com", "member@example.com");
        assertThrows(ResponseStatusException.class, () -> teams.get(team.id(), "member@example.com"));
        assertThrows(ResponseStatusException.class, () -> teams.note(team.id(), "member@example.com", "folder", "lead", "No access"));
        teams.join("member@example.com", rotated.inviteCode()); teams.leave(team.id(), "member@example.com");
        assertTrue(teams.list("member@example.com").isEmpty());
    }
    @Test void concurrentNotesAreNotLostAndCrossTeamLeadIdsFail() throws Exception {
        var team = teams.create("leader@example.com", "Sales"); teams.join("member@example.com", team.inviteCode());
        var shared = teams.importFile(team.id(), "leader@example.com", source("leader@example.com").id());
        var folder = shared.folders().getFirst(); var lead = folder.leads().getFirst();
        var other = teams.create("leader@example.com", "Other team");
        assertThrows(ResponseStatusException.class, () -> teams.note(other.id(), "leader@example.com", folder.id(), lead.id(), "Wrong team"));
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tasks = new ArrayList<Callable<Void>>();
            for (int i = 0; i < 8; i++) { final int number = i; tasks.add(() -> { teams.note(team.id(), number % 2 == 0 ? "leader@example.com" : "member@example.com", folder.id(), lead.id(), "Note " + number); return null; }); }
            for (var future : executor.invokeAll(tasks)) future.get();
        }
        assertEquals(8, teams.get(team.id(), "leader@example.com").folders().getFirst().leads().getFirst().notes().size());
        assertEquals(lead.data(), files.list("leader@example.com").getFirst().leads().getFirst());
    }
}
