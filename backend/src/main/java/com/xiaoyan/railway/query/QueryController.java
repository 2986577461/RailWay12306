package com.xiaoyan.railway.query;

import com.xiaoyan.railway.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/trains")
public class QueryController {
    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/search")
    public ApiResponse<Map<String, Object>> search(@RequestParam String from,
                                                   @RequestParam String to,
                                                   @RequestParam String date) {
        return ApiResponse.ok(queryService.search(from, to, date));
    }
}
