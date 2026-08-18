package com.example.jobs.worker.job;

import com.example.jobs.domain.JobMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JobDispatcher {

    private final Map<String, JobHandler> handlers;

    public JobDispatcher(List<JobHandler> handlers) {
        this.handlers = handlers.stream().
            collect(Collectors.toUnmodifiableMap(
                JobHandler::supportedType, 
                Function.identity()
        ));
    }

    public void dispatch(JobMessage message) {
        JobHandler handler = handlers.get(message.type());

        if (handler == null) {
            throw new IllegalArgumentException(
                "Unsupported job type: " + message.type()
            );
        }

        handler.handle(message);
    }
}