package com.aics.ticket.chat;

import com.aics.ticket.common.BusinessException;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 违规内容检测 —— 简单的关键词过滤
 */
@Service
public class ViolationGuardService {

    private static final List<String> SENSITIVE_WORDS = List.of(
        "傻逼", "操你", "草你", "去死", "滚蛋",
        "赌博", "博彩", "色情", "淫秽",
        "毒品", "摇头丸", "枪支",
        "台独", "港独", "法轮功"
    );

    public void check(String content) {
        if (content == null || content.isEmpty()) return;

        if (content.length() > 2000) {
            throw new BusinessException("咨询内容过长，请精简后重试");
        }

        for (String word : SENSITIVE_WORDS) {
            if (content.contains(word)) {
                throw new BusinessException("您的发言包含违规内容，请文明用语");
            }
        }
    }
}
