package com.wlx.ojbackendmodel.model.dto.exam;

import lombok.Data;
import java.io.Serializable;

@Data
public class OptionItem implements Serializable {
    private String key;
    private String value;
    private static final long serialVersionUID = 1L;
}
