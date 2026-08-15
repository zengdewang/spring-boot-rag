package com.example.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.rag.dto.SessionSummary;
import com.example.rag.entity.ChatRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatRecordMapper extends BaseMapper<ChatRecord> {

    /** 会话列表：按会话分组统计，取最近一条问题作预览，按最近活跃排序 */
    @Select("SELECT session_id AS sessionId, "
            + "COUNT(*) AS messageCount, "
            + "MAX(created_at) AS lastTime, "
            + "(SELECT question FROM chat_record q2 WHERE q2.session_id = session_id "
            + "  ORDER BY q2.id DESC LIMIT 1) AS lastQuestion "
            + "FROM chat_record GROUP BY session_id ORDER BY lastTime DESC")
    List<SessionSummary> listSessionSummaries();
}
