DocumentUltimate v3.5.1.0
ASP.NET Document Viewer and Converter
Copyright © 2016-2017 GleamTech
https://www.gleamtech.com/documentultimate

Version 3.5.1 Release Notes:
https://docs.gleamtech.com/documentultimate/html/version-history.htm#v3.5.1

Online Documentation:
https://docs.gleamtech.com/documentultimate/

Support Portal:
https://support.gleamtech.com/

------------------------------------------------------------------------------------------------------
To use DocumentUltimate in an ASP.NET MVC Project, do the following in Visual Studio:
------------------------------------------------------------------------------------------------------

1. Set DocumentUltimate's global configuration. For example, you may want to set the license key and 
    the cache path. Insert some of the following lines (if overriding a default value is required) into 
    the Application_Start method of your Global.asax.cs:

    ----------------------------------
    //Set this property only if you have a valid license key, otherwise do not
    //set it so DocumentUltimate runs in trial mode.
    DocumentUltimateConfiguration.Current.LicenseKey = "QQJDJLJP34...";

    //The default CachePath value is "~/App_Data/DocumentCache"
    //Both virtual and physical paths are allowed.
    DocumentUltimateWebConfiguration.Current.CachePath = "~/App_Data/DocumentCache";
    ----------------------------------

    Alternatively you can specify the configuration in <appSettings> tag of your Web.config.

    ----------------------------------
    <appSettings>
      <add key="DocumentUltimate:LicenseKey" value="QQJDJLJP34..." />
      <add key="DocumentUltimateWeb:CachePath" value="~/App_Data/DocumentCache" />
    </appSettings>
    ----------------------------------

    As you would notice, DocumentUltimate: prefix maps to DocumentUltimateConfiguration.Current and 
    DocumentUltimateWeb: prefix maps to DocumentUltimateWebConfiguration.Current.	   

2. Open one of your View pages (eg. Index.cshtml) and at the top of
    your page add the necessary namespaces:

    ----------------------------------
    @using GleamTech.Web.Mvc
    @using GleamTech.DocumentUltimate.Web
    ----------------------------------

    Alternatively you can add the namespaces globally in Views/web.config to avoid adding namespaces 
    in your pages every time:

    ----------------------------------
    <system.web.webPages.razor>
      <pages pageBaseType="System.Web.Mvc.WebViewPage">
        <namespaces>
          .
          .
          .
          <add namespace="GleamTech.Web.Mvc" />
          <add namespace="GleamTech.DocumentUltimate.Web" />
        </namespaces>
      </pages>
    </system.web.webPages.razor>
    ----------------------------------

    Now in your page insert these lines:

    ----------------------------------
    @{
        var documentViewer = new DocumentViewer 
        {
            Width = 800,
            Height = 600,
            Document = "~/Documents/Document.docx"
        };
    }
    <html> 
      <head> 
        @Html.RenderCss(documentViewer) 
        @Html.RenderJs(documentViewer)
      </head> 
      <body> 
        @Html.RenderControl(documentViewer) 
      </body> 
    </html>
    ----------------------------------

    This will render a DocumentViewer control in the page which loads and displays the source document 
    ~/Documents/Document.docx. Upon first view, internally DocumentViewer will convert the source document
    to PDF (used for "Download as Pdf" and also for next conversion step) and then to 
    XPZ (a special web-friendly format which DocumentViewer uses to actually render documents in the browser).
    So in this case the user will see "please wait awhile..." message in the viewer for a few seconds. 
    These generated PDF and XPZ files will be cached and upon consecutive page views, the document will be 
    served directly from the cache so the user will see the document instantly on second viewing. 
    When you modify the source document, the cached files are invalidated so your original document and 
    the corresponding cached files are always synced automatically. Note that it's also possible to 
    pre-cache documents via DocumentCache.PreCacheDocument method (e.g. when your user uploads a document), 
    see General Samples for more information.
    
------------------------------------------------------------------------------------------------------
To use DocumentUltimate in an ASP.NET WebForms Project, do the following in Visual Studio:
------------------------------------------------------------------------------------------------------

1. Set DocumentUltimate's global configuration. For example, you may want to set the license key and 
    the cache path. Insert some of the following lines (if overriding a default value is required) into 
    the Application_Start method of your Global.asax.cs:

    ----------------------------------
    //Set this property only if you have a valid license key, otherwise do not
    //set it so DocumentUltimate runs in trial mode.
    DocumentUltimateConfiguration.Current.LicenseKey = "QQJDJLJP34...";

    //The default CachePath value is "~/App_Data/DocumentCache"
    //Both virtual and physical paths are allowed.
    DocumentUltimateWebConfiguration.Current.CachePath = "~/App_Data/DocumentCache";
    ----------------------------------

    Alternatively you can specify the configuration in <appSettings> tag of your Web.config.

    ----------------------------------
    <appSettings>
      <add key="DocumentUltimate:LicenseKey" value="QQJDJLJP34..." />
      <add key="DocumentUltimateWeb:CachePath" value="~/App_Data/DocumentCache" />
    </appSettings>
    ----------------------------------

    As you would notice, DocumentUltimate: prefix maps to DocumentUltimateConfiguration.Current and 
    DocumentUltimateWeb: prefix maps to DocumentUltimateWebConfiguration.Current.	   
      

2. Open one of your pages (eg. Default.aspx) and at the top of your
    page add add the necessary namespaces:

    ----------------------------------
    <%@ Register TagPrefix="GleamTech" Namespace="GleamTech.DocumentUltimate.Web" Assembly="GleamTech.DocumentUltimate" %>
    ----------------------------------

    Alternatively you can add the namespaces globally in Web.config to avoid adding namespaces 
    in your pages every time:

    ----------------------------------
    <system.web>
      <pages>
        <controls>
          .
          .
          .
          <add tagPrefix="GleamTech" namespace="GleamTech.DocumentUltimate.Web" assembly="GleamTech.DocumentUltimate" />
        </controls>
      </pages>
    </system.web>
    ----------------------------------

    Now in your page insert these lines:

    ----------------------------------
    <GleamTech:DocumentViewer runat="server" 
        Width="800" 
        Height="600" 
        Document="~/Documents/Document.docx" />
     ----------------------------------

    This will render a DocumentViewer control in the page which loads and displays the source document 
    ~/Documents/Document.docx. Upon first view, internally DocumentViewer will convert the source document
    to PDF (used for "Download as Pdf" and also for next conversion step) and then to 
    XPZ (a special web-friendly format which DocumentViewer uses to actually render documents in the browser).
    So in this case the user will see "please wait awhile..." message in the viewer for a few seconds. 
    These generated PDF and XPZ files will be cached and upon consecutive page views, the document will be 
    served directly from the cache so the user will see the document instantly on second viewing. 
    When you modify the source document, the cached files are invalidated so your original document and 
    the corresponding cached files are always synced automatically. Note that it's also possible to 
    pre-cache documents via DocumentCache.PreCacheDocument method (e.g. when your user uploads a document), 
    see General Samples for more information.

