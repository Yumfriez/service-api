package com.epam.ta.reportportal.core.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;


/**
 * @author Konstantin Antipin
 */

@Component
@Aspect
public class HttpLoggingAspect {

	private static final String READABLE_CONTENT_TYPES = "text/plain text/html text/xml application/json application/xml application/hal+xml application/hal+json";

	private static final String NEWLINE = "\n";
	private static final String BODY_DENOMINATOR = "-- Body --";
	private static final String BODY_BINARY_MARK = "<binary body>";

	private static final AtomicLong COUNTER = new AtomicLong();

	@Value("${rp.requestLogging:false}")
	private boolean requestLoggingEnabled;

	@Autowired
	private ObjectMapper objectMapper;

	@Around("execution(public * *(..)) && @annotation(annotation)")
	public Object log(ProceedingJoinPoint joinPoint, HttpLogging annotation) throws Throwable {

		if (!requestLoggingEnabled) {
			return joinPoint.proceed();
		}

		Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());


		Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		Object requestBody = getBody(joinPoint, method);

		String prefix = method.getName();

		long requestCount = COUNTER.incrementAndGet();

		if (logger.isDebugEnabled()) {
			logger.debug(formatRequestRecord(requestCount, prefix, request, requestBody, annotation));
		}

		Object response;

		long start = System.currentTimeMillis();
		try {
			response = joinPoint.proceed();
			long executionTime = System.currentTimeMillis() - start;
			if (logger.isDebugEnabled()) {
				logger.debug(formatResponseRecord(requestCount, prefix, response, annotation, executionTime));
			}
		} catch (Throwable throwable) {
			logger.error(" (" + requestCount + ") - Error", throwable);
			throw throwable;
		}

		return response;
	}

	private Object getBody(ProceedingJoinPoint joinPoint, Method method) {
		Object body = null;
		Object[] args = joinPoint.getArgs();
		Parameter[] parameters = method.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			Object arg = args[i];

			if (arg != null) {
				if (arg instanceof MultipartHttpServletRequest) {
					body = BODY_BINARY_MARK;
					break;
				} else if (parameters[i].isAnnotationPresent(RequestBody.class)) {
					body = arg;
					break;
				}  else if (arg instanceof HttpEntity) {
					body = ((HttpEntity) arg).getBody();
					break;
				}
			}
		}
		return body;
	}

	private String formatRequestRecord(long count, String prefix, HttpServletRequest request,
			Object body, HttpLogging annotation) throws Exception {
		StringBuilder record = new StringBuilder();

		// uri
		record.append(prefix)
				.append(" (").append(count).append(')').append(" - Request")
				.append(NEWLINE).append(' ').append(request.getMethod())
				.append(' ').append(URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8.displayName()));

		// headers
		if (annotation.logHeaders()) {
			Enumeration<String> names = request.getHeaderNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				Enumeration<String> values = request.getHeaders(name);
				record.append(NEWLINE).append(' ').append(name).append(':');
				boolean comma = false;
				while (values.hasMoreElements()) {
					if (comma) {
						record.append(',');
					} else {
						comma = true;
					}
					record.append(' ').append(values.nextElement());
				}
			}
		}

		// body
		if (body != null && annotation.logRequestBody()) {
			record.append(NEWLINE).append(' ').append(BODY_DENOMINATOR)
					.append(NEWLINE).append(' ').append(objectMapper.writeValueAsString(body));
		}

		return record.toString();
	}

	private String formatResponseRecord(long count, String prefix, Object response, HttpLogging annotation, long executionTime) throws Exception {
		boolean binaryBody = false;
		StringBuilder record = new StringBuilder();

		record.append(prefix).append(" (").append(count).append(')').append(" - Response ").append(" (").append(executionTime).append(" ms)");

		if (response instanceof ResponseEntity) {
			HttpStatus status = ((ResponseEntity) response).getStatusCode();
			record.append(NEWLINE).append(' ').append(status).append(" - ").append(status.getReasonPhrase());

			if (annotation.logHeaders()) {
				HttpHeaders headers = ((ResponseEntity) response).getHeaders();
				for (String name : headers.keySet()) {
					record.append(NEWLINE).append(' ').append(name).append(':');
					boolean comma = false;
					for (String value : headers.get(name)) {
						if (HttpHeaders.CONTENT_TYPE.equals(name) && !readableContent(value)) {
							binaryBody = true;
						}
						if (comma) {
							record.append(',');
						} else {
							comma = true;
						}
						record.append(' ').append(value);
					}
				}
			}

			if (annotation.logResponseBody()) {
				record.append(NEWLINE).append(' ').append(BODY_DENOMINATOR);
				if (binaryBody) {
					record.append(NEWLINE).append(' ').append('"').append(BODY_BINARY_MARK).append('"');
				} else {
					try {
						record.append(NEWLINE).append(' ').append(objectMapper.writeValueAsString(((ResponseEntity<?>) response).getBody()));
					} catch (JsonProcessingException ex) {
						record.append(NEWLINE).append(' ').append(((ResponseEntity<String>) response).getBody());
					}
				}
			}
		} else {
			record.append(NEWLINE).append(' ').append("Status").append(" - ").append("OK (method return)");
			record.append(NEWLINE).append(' ').append(BODY_DENOMINATOR);
			try {
				record.append(NEWLINE).append(' ').append(objectMapper.writeValueAsString(response));
			} catch (JsonProcessingException ex) {
				// ignore
			}
		}

		return record.toString();
	}

	private boolean readableContent(String value) {
		int idx = value.indexOf(';');
		return READABLE_CONTENT_TYPES.contains(value.substring(0, idx > 0 ? idx : value.length()));
	}
}
