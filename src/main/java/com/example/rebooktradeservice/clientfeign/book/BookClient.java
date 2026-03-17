package com.example.rebooktradeservice.clientfeign.book;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "book-service")
public interface BookClient {
    @GetMapping("/internal/books/recommendations/{userId}")
    List<Long> getRecommendedBooks(@PathVariable String userId);
}
