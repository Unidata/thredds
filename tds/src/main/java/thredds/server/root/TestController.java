/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.root;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.ResponseBody;

import thredds.servlet.ServletUtil;

/**
 * Prints and returns request details.
 * Use only for testing and debugging purposes
 * 
 * @author caron
 * @since 7/5/11
 */
//@Controller
public class TestController {

	//@RequestMapping(value="/test/**")
	public @ResponseBody String getRequestDetails(HttpServletRequest req,	HttpServletResponse res) throws Exception {

		String path = req.getPathInfo();
		if (path == null)
			path = "";
		String requestDetails = ServletUtil.showRequestDetail(req);
		System.out.printf("%s%n", requestDetails);
		return requestDetails;
	}
}
