package com.caojing;

import com.alibaba.fastjson.JSONObject;
import lombok.Setter;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.caojing.HttpUtils.doGet;

/**
 * 启动类
 *
 * @author CaoJing
 * @date 2020/02/12 01:23
 */
@RestController
@SpringBootApplication
public class Application {

    @Setter
    public static String proToken;

    @Setter
    public static String devToken;

    /**
     * 初始化token
     */
    public Application() throws IOException {
        InputStream in1 = Application.class.getClassLoader().getResourceAsStream("pro-token.txt");
        proToken = new BufferedReader(new InputStreamReader(Objects.requireNonNull(in1))).readLine();

        InputStream in2 = Application.class.getClassLoader().getResourceAsStream("dev-token.txt");
        devToken = new BufferedReader(new InputStreamReader(Objects.requireNonNull(in2))).readLine();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * 更新token
     */
    @PostMapping("/updateToken")
    public String updateToken(@RequestHeader String proToken,
                              @RequestHeader String devToken) {
        setProToken(proToken);
        setDevToken(devToken);
        return "更新成功";
    }

    /**
     * 代理GET请求
     *
     * @param request 请求体
     * @return JSONObject
     * @author CaoJing
     * @date 2020/02/22 20:07:08
     */
    @PostMapping("/proxyGet")
    public JSONObject proxyGet(@RequestBody JSONObject request) throws Exception {
        Map<String, String> headers = new HashMap<>(2);
        String url = request.getString("url");
        headers.put("token", url.startsWith("https://alphalawyer.cn") ? proToken : devToken);
        return JSONObject.parseObject(EntityUtils.toString(doGet(headers, url).getEntity()));
    }

    /**
     * 查询生产环境案例jid集合
     *
     * @param request 请求体
     * @return List
     * @author CaoJing
     * @date 2020/02/21 15:33:37
     */
    @PostMapping("/getJidList")
    public List<String> getJidList(@RequestBody JSONObject request) throws Exception {
        return proxyGet(request)
            .getJSONObject("result")
            .getJSONArray("judgements")
            .stream()
            .map(x -> (JSONObject) x)
            .map(y -> y.getString("jid"))
            .collect(Collectors.toList());
    }
}
