package com.easychat.utils;

import org.junit.Assert;
import org.junit.Test;

public class SessionIdUtilsTest {

    @Test
    public void generatePrivateSessionIdShouldIgnoreUserOrder() {
        String first = SessionIdUtils.generatePrivateSessionId(1, 2);
        String second = SessionIdUtils.generatePrivateSessionId(2, 1);

        Assert.assertEquals(first, second);
    }

    @Test
    public void generateGroupSessionIdShouldOnlyDependOnGroupId() {
        Assert.assertEquals("G_100", SessionIdUtils.generateGroupSessionId(100));
    }

    @Test
    public void privateAndGroupSessionIdsShouldNotCollideByFormat() {
        String privateSessionId = SessionIdUtils.generatePrivateSessionId(1, 100);
        String groupSessionId = SessionIdUtils.generateGroupSessionId(100);

        Assert.assertNotEquals(privateSessionId, groupSessionId);
    }
}
