package com.scraper.leadscraper.history;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class LeadFiles {
    public record File(String id, String name, String createdAt, String updatedAt, String method, String query, List<SearchHistory.Lead> leads) { }
    private final Path directory; private final ObjectMapper mapper; private final SearchHistory history;
    public LeadFiles(@Value("${heimdall.files.directory:.data/lead-files}") String directory, ObjectMapper mapper, SearchHistory history) { this.directory=Path.of(directory); this.mapper=mapper; this.history=history; }
    private Path path(String email) { try { return directory.resolve(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(email.getBytes(StandardCharsets.UTF_8)))+".json"); } catch(Exception e) { throw new IllegalStateException(e); } }
    private String name(String input) { if(input==null || input.isBlank() || input.strip().length()>100 || input.chars().anyMatch(Character::isISOControl)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a file name of 1–100 characters without control characters."); return input.strip(); }
    public synchronized List<File> list(String email) { try { Path path=path(email); return Files.exists(path)?List.of(mapper.readValue(Files.readAllBytes(path),File[].class)):List.of(); } catch(Exception e) { throw new IllegalStateException("Cannot load lead files",e); } }
    private void write(String email,List<File> files) { Path temporary=null; try { Files.createDirectories(directory);temporary=Files.createTempFile(directory,"leads-",".tmp");try {Files.setPosixFilePermissions(temporary,PosixFilePermissions.fromString("rw-------"));}catch(UnsupportedOperationException ignored){} Files.write(temporary,mapper.writeValueAsBytes(files));Files.move(temporary,path(email),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }catch(Exception e){throw new IllegalStateException("Cannot save lead files",e);}finally{if(temporary!=null)try{Files.deleteIfExists(temporary);}catch(Exception ignored){}} }
    public synchronized File create(String email,String input,String runId) { String name=name(input);List<File> files=new ArrayList<>(list(email));if(files.size()>=100)throw new ResponseStatusException(HttpStatus.CONFLICT,"Your library can hold up to 100 lead files.");var run=history.list(email).stream().filter(r->r.id().equals(runId)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"This search is no longer in your saved history."));if(run.leads().isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This search has no leads to save.");String now=Instant.now().toString();File file=new File(UUID.randomUUID().toString(),name,now,now,run.method(),run.query(),List.copyOf(run.leads()));files.add(0,file);write(email,files);return file; }
    public synchronized File rename(String email,String id,String input) {String name=name(input);List<File> files=new ArrayList<>(list(email));for(int i=0;i<files.size();i++){File old=files.get(i);if(old.id().equals(id)){File next=new File(id,name,old.createdAt(),Instant.now().toString(),old.method(),old.query(),old.leads());files.set(i,next);write(email,files);return next;}}throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Lead file not found.");}
}
