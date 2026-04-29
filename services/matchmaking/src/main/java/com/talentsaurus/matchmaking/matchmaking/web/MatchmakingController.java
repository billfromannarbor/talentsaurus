package com.talentsaurus.matchmaking.matchmaking.web;

import com.talentsaurus.matchmaking.matchmaking.dto.FitRequest;
import com.talentsaurus.matchmaking.matchmaking.dto.FitResponse;
import com.talentsaurus.matchmaking.matchmaking.service.MatchmakingService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matchmaking")
public class MatchmakingController {

  private final MatchmakingService matchmakingService;

  public MatchmakingController(MatchmakingService matchmakingService) {
    this.matchmakingService = matchmakingService;
  }

  @PostMapping(path = "/match", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FitResponse> match(@Valid @RequestBody FitRequest request) {
    return ResponseEntity.ok(matchmakingService.match(request));
  }
}
