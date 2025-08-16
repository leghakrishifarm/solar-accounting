package com.legakrishi.solar.iot;

import com.legakrishi.solar.iot.dto.IngestPayload;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.*;

@RestController("iotIngestController") // <-- unique bean name
@RequestMapping("/iot/ingest")          // <-- keep endpoints distinct
public class IngestController {

    private final IngestService service;

    public IngestController(IngestService service) {
        this.service = service;
    }

    @PostMapping("/ingest")
    public ResponseEntity<?> ingest(@RequestBody IngestPayload payload) {
        try {
            service.ingest(payload);
            return ResponseEntity.ok().body("OK");
        } catch (SecurityException se) {
            return ResponseEntity.status(401).body("Unauthorized");
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(iae.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Failed: " + ex.getMessage());
        }
    }
}
