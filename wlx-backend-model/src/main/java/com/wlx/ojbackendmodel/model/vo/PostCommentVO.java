package com.wlx.ojbackendmodel.model.vo;

import com.wlx.ojbackendmodel.model.entity.PostComment;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 帖子评论视图
 */
@Data
public class PostCommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long parentId;

    private Long postId;

    private String content;

    private UserVO userVO;

    private Date createTime;

    /**
     * 包装类转对象
     */
    public static PostComment voToObj(PostCommentVO vo) {
        if (vo == null) {
            return null;
        }
        PostComment comment = new PostComment();
        BeanUtils.copyProperties(vo, comment);
        return comment;
    }

    /**
     * 对象转包装类
     */
    public static PostCommentVO objToVo(PostComment comment) {
        if (comment == null) {
            return null;
        }
        PostCommentVO vo = new PostCommentVO();
        BeanUtils.copyProperties(comment, vo);
        return vo;
    }
}


