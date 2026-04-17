package com.jacuum.web;

import com.jacuum.engine.SessionView;
import com.jacuum.web.dto.CreateSessionRequest;
import com.jacuum.web.dto.SessionResponse;

interface SessionApi {
    SessionResponse create(CreateSessionRequest req) throws Exception;
    SessionView start(String id) throws Exception;
    SessionView pause(String id) throws Exception;
    SessionView resume(String id) throws Exception;
    SessionView stop(String id) throws Exception;
    SessionView view(String id) throws Exception;
}
