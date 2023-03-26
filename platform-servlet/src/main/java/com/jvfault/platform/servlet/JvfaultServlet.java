package com.jvfault.platform.servlet;

import com.jvfault.web.WebApplication;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * jvfault 分发 Servlet - 把 Servlet 请求桥接到 WebApplication。
 *
 * <p>web.xml 或 @WebServlet("/\u002a") 注册即可接入任意 Servlet 容器。
 *
 * @since v0.4.0 (2018)
 * @author jvfault team
 */
public class JvfaultServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(JvfaultServlet.class);

    private final transient WebApplication application;

    public JvfaultServlet(WebApplication application) {
        this.application = application;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long start = System.nanoTime();
        ServletRequestBridge requestBridge = new ServletRequestBridge(req);
        ServletResponseBridge responseBridge = new ServletResponseBridge(resp);
        try {
            application.dispatch(new com.jvfault.web.http.HttpContext(requestBridge, responseBridge));
        } catch (Throwable t) {
            log.error("请求分发失败: {}", req.getRequestURI(), t);
            if (!resp.isCommitted()) {
                resp.sendError(500, "Internal Server Error");
            }
            return;
        }
        responseBridge.flush();
        if (log.isDebugEnabled()) {
            log.debug("{} {} -> {} ({}ms)", req.getMethod(), req.getRequestURI(),
                    responseBridge.getStatus(), (System.nanoTime() - start) / 1_000_000);
        }
    }
}
