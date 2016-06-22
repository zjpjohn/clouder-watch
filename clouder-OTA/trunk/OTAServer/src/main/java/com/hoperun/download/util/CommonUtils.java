package com.hoperun.download.util;

import java.util.UUID;

/**
 * 
 * @author loven
 * 
 */
public final class CommonUtils {

    /**
     * 
     * @param obj
     * @return
     */
    public static Integer convertToInt(Object obj) {
        Integer rel = null;
        
        if(obj != null)
        {
            rel = Integer.parseInt(obj.toString());
        }

        return rel;
    }
    
    /**
     * 
     * @return
     */
    public static String uuid(){
        return UUID.randomUUID().toString();
    }
}
