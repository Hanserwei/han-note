import com.hanserwei.hannote.user.biz.HannoteUserBizApplication;
import com.hanserwei.hannote.user.biz.constant.RedisKeyConstants;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest(classes = HannoteUserBizApplication.class)
public class HannoteIdGenerateTest {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void test() {
//        Long id = redisTemplate.opsForValue().increment(RedisKeyConstants.HAN_NOTE_ID_GENERATOR_KEY);
        Object id = redisTemplate.opsForValue().get(RedisKeyConstants.HAN_NOTE_ID_GENERATOR_KEY);
        System.out.println(id);
    }
}
