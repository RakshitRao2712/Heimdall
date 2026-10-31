package com.scraper.leadscraper.teams;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/workspace/teams")
public class TeamController {
    public record Create(String name) { }
    public record Join(String code) { }
    public record Import(String fileId) { }
    public record Status(Boolean done, Long version) { }
    public record Note(String text) { }
    public record Member(String email) { }
    private final TeamStore teams;
    public TeamController(TeamStore teams) { this.teams = teams; }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String, String>> error(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("detail", e.getReason() == null ? "Could not update this team." : e.getReason())); }
    @GetMapping public List<TeamStore.Summary> list(Principal user) { return teams.list(user.getName()); }
    @PostMapping public TeamStore.View create(@RequestBody Create input, Principal user) { return teams.create(user.getName(), input.name()); }
    @PostMapping("/join") public TeamStore.View join(@RequestBody Join input, Principal user) { return teams.join(user.getName(), input.code()); }
    @GetMapping("/{id}") public TeamStore.View get(@PathVariable String id, Principal user) { return teams.get(id, user.getName()); }
    @PostMapping("/{id}/invite") public TeamStore.View rotate(@PathVariable String id, Principal user) { return teams.rotate(id, user.getName()); }
    @PostMapping("/{id}/members/remove") public ResponseEntity<Void> remove(@PathVariable String id, @RequestBody Member input, Principal user) { teams.removeMember(id, user.getName(), input.email()); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/leave") public ResponseEntity<Void> leave(@PathVariable String id, Principal user) { teams.leave(id, user.getName()); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/folders") public TeamStore.View share(@PathVariable String id, @RequestBody Import input, Principal user) { return teams.importFile(id, user.getName(), input.fileId()); }
    @PatchMapping("/{id}/folders/{folderId}/leads/{leadId}") public TeamStore.View status(@PathVariable String id, @PathVariable String folderId, @PathVariable String leadId, @RequestBody Status input, Principal user) {
        if (input.done() == null || input.version() == null || input.version() < 0) throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Choose a status and provide the current lead version.");
        return teams.status(id, user.getName(), folderId, leadId, input.done(), input.version());
    }
    @PostMapping("/{id}/folders/{folderId}/leads/{leadId}/notes") public TeamStore.View note(@PathVariable String id, @PathVariable String folderId, @PathVariable String leadId, @RequestBody Note input, Principal user) { return teams.note(id, user.getName(), folderId, leadId, input.text()); }
}
