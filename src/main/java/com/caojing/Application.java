package com.caojing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

import static com.caojing.HBaseKit.findOne;

/**
 * 启动类
 *
 * @author CaoJing
 * @date 2020/02/12 01:23
 */
@RestController
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * 查询
     */
    @GetMapping("/{table}/{rowKey}")
    public Map<String, String> get(@PathVariable String table,
                                   @PathVariable String rowKey) throws IOException {

        return findOne(table, rowKey);
    }
}
