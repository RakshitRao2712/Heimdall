package com.scraper.leadscraper.history;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
@RestController
@RequestMapping("/api/workspace/files")
public class LeadFilesController {
    public record Create(String name,String runId) { }
    public record Rename(String name) { }
    private final LeadFiles files;
    public LeadFilesController(LeadFiles files){this.files=files;}
    @GetMapping public List<LeadFiles.File> list(Principal user){return files.list(user.getName());}
    @PostMapping public LeadFiles.File create(@RequestBody Create input,Principal user){return files.create(user.getName(),input.name(),input.runId());}
    @PatchMapping("/{id}") public LeadFiles.File rename(@PathVariable String id,@RequestBody Rename input,Principal user){return files.rename(user.getName(),id,input.name());}
}
