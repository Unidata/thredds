package thredds.server.jupyter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import thredds.core.TdsRequestedDataset;
import thredds.server.config.JupyterConfigBean;
import thredds.server.config.TdsContext;
import thredds.util.TdsPathUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Controller
@RequestMapping("/jupyter")
public class JupyterKernelController {

    @Autowired
    TdsContext tdsContext;

    @Autowired
    JupyterConfigBean jupyterConfig;

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public String handleException(Exception exc) {
        StringWriter sw = new StringWriter(5000);
        exc.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    @RequestMapping(value="**")
    @ResponseBody
    public HttpEntity<byte[]> dataRequest(@RequestParam(value="method", required=false) String method,
                                          final HttpServletRequest req) throws IOException {

        if (method == null) {
            return capabilitiesRequest();
        }

        JupyterClient client = new JupyterClient(jupyterConfig.getPythonPath(),
                jupyterConfig.getTimeout());
        String datasetPath = TdsPathUtils.extractPath(req, "/jupyter");
        String dataFile = TdsRequestedDataset.getFile(datasetPath).toString();
        byte [] out = new byte[16];
        if (client.connect(tdsContext.getThreddsDirectory().toPath(), method)) {
            Map<String, String[]> params = new HashMap<>(req.getParameterMap());
            params.remove("method");
            String outputFileName = client.processFile(dataFile, params);
            File outputFile = new File(outputFileName);
            FileInputStream fis = new FileInputStream(outputFile);
            out = new byte[(int) outputFile.length()];
            fis.read(out);
            fis.close();
            outputFile.delete();
        }
        client.close();

        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "x-netcdf"));
        header.setContentLength(out.length);
        header.setContentDispositionFormData("attachment", method + ".nc4");
        return new HttpEntity<>(out, header);
    }

    @RequestMapping(value="capabilities")
    @ResponseBody
    public HttpEntity<byte[]> capabilitiesRequest() throws IOException {
        List<String> methods = JupyterClient.methods(tdsContext.getThreddsDirectory().toPath());
        StringJoiner builder = new StringJoiner("\n");
        methods.forEach(builder::add);
        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("text", "plain"));
        return new HttpEntity<>(builder.toString().getBytes(), header);
    }
}
