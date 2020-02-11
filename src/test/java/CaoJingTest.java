import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static com.caojing.HBaseKit.deleteRow;
import static com.caojing.HBaseKit.findOne;

/**
 * 非spring测试
 *
 * @author CaoJing
 * @date 2020/02/12 01:19
 */
public class CaoJingTest {

    /**
     * 查询
     */
    @Test
    public void test20200106214811() throws Exception {
        final Map<String, String> map = findOne("judgement_ds", "35C0D1C8A8729AAC804A055C74E25055");
        System.out.println(JSON.toJSONString(map, SerializerFeature.PrettyFormat));
    }

    /**
     * 删除
     */
    @Test
    public void test20200211143947() throws IOException {
        // 删除
        deleteRow("judgement_ds", "2B2A113088A9BCC6F8DF9C258490A3F7");
        // 查询
        final Map<String, String> map = findOne("judgement_ds", "2B2A113088A9BCC6F8DF9C258490A3F7");
        System.out.println(JSON.toJSONString(map, SerializerFeature.PrettyFormat));
    }
}
