/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.shell.example.console;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.wl4g.shell.common.annotation.ShellMethod;
import com.wl4g.shell.common.annotation.ShellMethod.InterruptType;
import com.wl4g.shell.common.annotation.ShellOption;
import com.wl4g.shell.core.handler.ProgressShellContext;
import com.wl4g.shell.core.handler.ProgressShellContext.UserShellContextBinders;
import com.wl4g.shell.core.handler.SimpleShellContext;
import com.wl4g.shell.example.console.args.MixedArgument;
import com.wl4g.shell.example.console.args.SumArgument;
import com.wl4g.shell.example.service.ExampleService;
import com.wl4g.shell.springboot.annotation.ShellComponent;

/**
 * Example console.</br>
 * Note: for the shell program to take effect, you must inject {@link Component}
 * or {@link Service} or {@link Bean} into the spring
 * {@link ApplicationContext}.
 * 
 * @author Wangl.sir &lt;Wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0.0 2019-04-21
 * @since
 */
@Component
@ShellComponent
public class ExampleConsole {
    final private static String GROUP_NAME = "Example commands";
    final private static Logger log = LoggerFactory.getLogger(ExampleConsole.class);

    @Autowired
    private ExampleService exampleService;

    // --------------- Injection arguments tests. ----------------

    /**
     * For example: $> sum1 -a 1 -b 123
     */
    @ShellMethod(keys = "sum1", group = GROUP_NAME, help = "Shell method tests that do not output results")
    public void sum1(SumArgument arg) {
        exampleService.add(arg);
    }

    /**
     * For example: $> sum2 -a 1 -b 123
     */
    @ShellMethod(keys = "sum2", group = GROUP_NAME, help = "Test of shell method that can output results")
    public void sum2(SimpleShellContext context, @ShellOption(opt = "a", lopt = "add1", help = "Add number") int a,
            @ShellOption(opt = "b", lopt = "add2", help = "Added number", defaultValue = "1") int b) {
        context.printf(exampleService.add(new SumArgument(a, b)).toString());
        context.completed();
    }

    /**
     * For example: $> set -l 1,2 -s x3,x4
     */
    @ShellMethod(keys = "set", group = GROUP_NAME, help = "Complex parameter injection testing")
    public void set(SimpleShellContext context,
            @ShellOption(opt = "s", lopt = "set", help = "Set<String> type argument field") Set<String> set1,
            @ShellOption(opt = "l", lopt = "list", help = "List<Integer> type argument field") List<Integer> list) {
        context.printf("Direct mixed set parameter injection results: set=" + set1 + ", list=" + list);
        context.completed();
    }

    /**
     * For example: $> mixed -l x1,x2 -m a1=b1,a2=b2 -p aa1=bb1,aa2=bb2 -s x3,x4
     * -e false -E true
     */
    @ShellMethod(keys = "mixed", group = GROUP_NAME, help = "Mixed parameter injection testing")
    public void mixed(MixedArgument arg, SimpleShellContext context) {
        context.printf("The input parameters are: " + arg.toString());
        context.completed();
    }

    // --------------- ShellContext using and binding tests. ----------------

    /**
     * For example: $> task1 -n 20
     */
    @ShellMethod(keys = "task1", group = GROUP_NAME, help = "This is a shell method for printing logs asynchronously.(Not support interrupt)")
    public void task1(
            @ShellOption(opt = "n", lopt = "num", required = false, defaultValue = "5", help = "Number of printed messages") int num,
            SimpleShellContext context) {

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                context.printf("Log print...");
                for (int i = 1; i <= num; i++) {
                    String message = "This is the " + i + "th message!";
                    log.info(message);

                    // Print to console
                    context.printf(message);

                    try {
                        Thread.sleep(200L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                context.printf("Log print finished!");
            } finally {
                // *** Note: Don't forget to execute it, or the client console
                // will pause until it timesout.
                context.completed();
            }
        });
    }

    /**
     * For example: $> task2 -n 20
     */
    @ShellMethod(keys = "task2", group = GROUP_NAME, interruptible = InterruptType.ALLOW, help = "This is a shell method for printing logs asynchronously.(Support interrupt)")
    public void task2(
            @ShellOption(opt = "n", lopt = "num", required = false, defaultValue = "5", help = "Number of printed messages") int num,
            ProgressShellContext context) {

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                context.printf("Log2 print...", 0.05f);
                for (int i = 1; !context.isInterrupted() && i <= num; i++) {
                    String message = "This is the " + i + "th message!";
                    log.info(message);

                    // Print to console
                    context.printf(message, num, i);

                    try {
                        Thread.sleep(200L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                // *** Note: Don't forget to execute it, or the client console
                // will pause until it timesout.
                context.completed("Log2 print finished!");
            }
        });
    }

    /**
     * For example: $> task3 -n 20
     */
    @ShellMethod(keys = "task3", group = GROUP_NAME, interruptible = InterruptType.ALLOW, help = "This is a simple shell method for printing logs synchronously.(Support interrupt)")
    public void task3(
            @ShellOption(opt = "n", lopt = "num", required = false, defaultValue = "5", help = "Number of printed messages") int num,
            @ShellOption(opt = "s", lopt = "sleep", required = false, defaultValue = "100", help = "Print message delay(ms)") long sleep,
            ProgressShellContext context) {

        // For testing the customization binding context
        UserShellContextBinders.bind(context);

        // Call real execution
        doTask3(num, sleep);
    }

    private static void doTask3(int num, long sleep) {
        ProgressShellContext context = UserShellContextBinders.get();
        try {
            context.printf("Log3 print...", 0.05f);
            for (int i = 1; !context.isInterrupted() && i <= num; i++) {
                String message = "This is the " + i + "th message!";
                log.info(message);

                // Print to console
                context.printf(message, num, i);

                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            // *** Note: Don't forget to execute it, or the client console
            // will pause until it timesout.
            context.completed("Log3 print finished!");
        }
    }

    // --------------- States switch tests. ----------------

    /**
     * For example: $> task4
     */
    @ShellMethod(keys = "task4", group = GROUP_NAME, help = "This is a test execution exception command methods")
    public void task4(SimpleShellContext context) {
        context.printf("Log print...");
        throw new IllegalStateException("This is a deliberate error!");
    }

    public static void main(String[] args) {
        doTask3(30, 200L);
    }

}