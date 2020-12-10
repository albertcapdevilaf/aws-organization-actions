package com.amazonaws.lambda.organizationActions;

import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.lambda.organizationActions.NukeAccounts;
import com.amazonaws.lambda.organizationActions.NukeRequest;
import com.amazonaws.lambda.organizationActions.TestContext;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class NukeAccountsTest {

    private static NukeRequest input;

    @BeforeClass
    public static void createInput() throws IOException {
        // TODO: set up your sample input object here.
        input = new NukeRequest();
        input.setRegion("us-east-1");
        input.setPrefix("workshop40");
        input.setSkipGuardDuty(false);
        input.setSkipSecHub(false);
    }

    private Context createContext() {
        TestContext ctx = new TestContext();

        // TODO: customize your context here if needed.
        ctx.setFunctionName("Test Lambda");

        return ctx;
    }

    @Test
    public void testNukeAccounts() {
        NukeAccounts handler = new NukeAccounts();
        Context ctx = createContext();

        String output = handler.handleRequest(input, ctx);

        // TODO: validate output here if needed.
        Assert.assertEquals("1 accounts processed", output);
    }
}
