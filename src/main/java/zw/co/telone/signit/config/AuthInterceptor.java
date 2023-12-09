package zw.co.telone.signit.config;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

       if (request.getMethod().equals("OPTIONS")) {
//            // Allow OPTIONS requests without further processing
           return true;
       }
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            boolean isTokenValid = jwtUtil.validateToken(token);

            if (isTokenValid) {

                return true;
            }
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        return false;
    }
}