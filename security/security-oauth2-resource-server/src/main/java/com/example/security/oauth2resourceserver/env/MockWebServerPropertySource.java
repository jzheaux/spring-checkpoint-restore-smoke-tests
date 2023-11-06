/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.security.oauth2resourceserver.env;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Adds value for mockwebserver.url property.
 *
 * @author Rob Winch
 */
public class MockWebServerPropertySource extends PropertySource<AtomicReference<MockWebServer>>
		implements DisposableBean, Resource {

	private static final MockResponse JWKS_RESPONSE = response(
			"{ \"keys\": [ { \"kty\": \"RSA\", \"e\": \"AQAB\", \"n\": \"jvBtqsGCOmnYzwe_-HvgOqlKk6HPiLEzS6uCCcnVkFXrhnkPMZ-uQXTR0u-7ZklF0XC7-AMW8FQDOJS1T7IyJpCyeU4lS8RIf_Z8RX51gPGnQWkRvNw61RfiSuSA45LR5NrFTAAGoXUca_lZnbqnl0td-6hBDVeHYkkpAsSck1NPhlcsn-Pvc2Vleui_Iy1U2mzZCM1Vx6Dy7x9IeP_rTNtDhULDMFbB_JYs-Dg6Zd5Ounb3mP57tBGhLYN7zJkN1AAaBYkElsc4GUsGsUWKqgteQSXZorpf6HdSJsQMZBDd7xG8zDDJ28hGjJSgWBndRGSzQEYU09Xbtzk-8khPuw\" } ] }",
			200);

	private static final MockResponse NOT_FOUND_RESPONSE = response(
			"{ \"message\" : \"This mock authorization server responds to just one request: GET /.well-known/jwks.json.\" }",
			404);

	/**
	 * Name of the random {@link PropertySource}.
	 */
	public static final String MOCK_WEB_SERVER_PROPERTY_SOURCE_NAME = "mockwebserver";

	private static final String NAME = "mockwebserver.url";

	private static final Log logger = LogFactory.getLog(MockWebServerPropertySource.class);

	private boolean started;

	private int port;

	public MockWebServerPropertySource() {
		super(MOCK_WEB_SERVER_PROPERTY_SOURCE_NAME, new AtomicReference<>(new MockWebServer()));
		logger.info("Initializing MockWebServerPropertySource");
		Core.getGlobalContext().register(this);
	}

	@Override
	public Object getProperty(String name) {
		if (!name.equals(NAME)) {
			return null;
		}
		logger.trace("Looking up the url for '%s'".formatted(name));
		try {
			String url = getUrl();
			logger.trace("Property value: '%s' = '%s'".formatted(name, url));
			return url;
		}
		catch (RuntimeException ex) {
			logger.error("Failed to get property value for '%s'".formatted(name), ex);
			throw ex;
		}
	}

	@Override
	public void destroy() throws Exception {
		getSource().get().shutdown();
		this.started = false;
	}

	@Override
	public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
		destroy();
		getSource().set(new MockWebServer());
	}

	@Override
	public void afterRestore(Context<? extends Resource> context) throws Exception {
		getUrl();
	}

	/**
	 * Gets the URL (i.e. "http://localhost:123456")
	 * @return the url with the dynamic port
	 */
	private String getUrl() {
		MockWebServer mockWebServer = getSource().get();
		if (!this.started) {
			initializeMockWebServer(mockWebServer);
		}
		String url = mockWebServer.url("").url().toExternalForm();
		return url.substring(0, url.length() - 1);
	}

	private void initializeMockWebServer(MockWebServer mockWebServer) {
		Dispatcher dispatcher = new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				if ("/.well-known/jwks.json".equals(request.getPath())) {
					return JWKS_RESPONSE;
				}

				return NOT_FOUND_RESPONSE;
			}
		};

		mockWebServer.setDispatcher(dispatcher);
		try {
			mockWebServer.start(this.port);
			this.started = true;
			this.port = mockWebServer.getPort();
		}
		catch (IOException ex) {
			throw new RuntimeException("Could not start " + mockWebServer, ex);
		}
	}

	private static MockResponse response(String body, int status) {
		return new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.setResponseCode(status)
			.setBody(body);
	}

}
