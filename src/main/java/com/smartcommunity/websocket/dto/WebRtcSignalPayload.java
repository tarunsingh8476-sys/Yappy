package com.smartcommunity.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebRtcSignalPayload {

    /**
     * Signal types:
     * - CALL_REQUEST: Initiating 1:1 call
     * - CALL_ACCEPTED: Callee accepted call
     * - CALL_REJECTED: Callee declined call
     * - OFFER: WebRTC SDP Offer
     * - ANSWER: WebRTC SDP Answer
     * - ICE_CANDIDATE: ICE candidate for NAT traversal
     * - JOIN_ROOM: User joined a community voice/video room
     * - LEAVE_ROOM: User left a community voice/video room
     * - HANG_UP: Terminate call
     */
    private String type;

    private Long senderId;
    private String senderUsername;

    private Long targetUserId;

    private String roomId; // Channel ID or Conversation ID

    private Object sdp; // SDP offer or answer object

    private Object candidate; // ICE candidate data

    private boolean isVideo;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
