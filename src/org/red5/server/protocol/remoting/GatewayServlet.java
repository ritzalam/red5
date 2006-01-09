package org.red5.server.protocol.remoting;

public class GatewayServlet {} /* extends HttpServlet extends HttpServlet {

	private static final Log log = LogFactory.getLog(GatewayServlet.class);
	protected RemotingService remoting = null;
	protected ApplicationContext app = null;
	protected static final String REMOTING_SERVICE = "remotingService";
	protected static final String AMF_CONTENT_TYPE = "application/x-amf";
	
	public void init() throws ServletException {
		if(log.isInfoEnabled())
			log.info("Initialize: "+getServletInfo());
		app = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		remoting = (RemotingService) app.getBean(REMOTING_SERVICE);
	}
	
	public void service(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
	
		String contentType = req.getContentType();
		if(contentType != null && contentType.equals(AMF_CONTENT_TYPE)){
			serviceAMF(req,resp);
		} else {
			if(req.getParameter("data")!=null){
				serviceText(req,resp);
			} else serviceBrowser(req,resp);
		}
	
	}
	
	protected void serviceAMF(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		initializeRequestContext(req,resp);
		
		DataInputStream din = new DataInputStream(req.getInputStream());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		remoting.service(new AMFDataInputStream(din),new AMFDataOutputStream(dos));
		
		resp.setContentType("application/x-amf");
		resp.setContentLength(baos.size());
		ServletOutputStream sos = resp.getOutputStream();
		baos.writeTo(sos);
		sos.flush();
	}
	
	protected void serviceBrowser(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		PrintWriter out = resp.getWriter();
		resp.setContentType("text/plain");
		out.println(getServletInfo());
		out.flush();
		out.close();
	}
	
	public String getServletInfo() {
		return "[red5] remoting gateway servlet, http://www.osflash.org/red5 http://www.red5.org";
	}
	
}*/
