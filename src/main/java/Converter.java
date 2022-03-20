import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * Author: Angel Legaspi
 */
public class Converter
{
    public static void main(String[] args)
    {
        if(args.length == 0)
            System.out.println("Usage: java Converter <xml file name> <xsl file name>");
        else if(args.length == 1)
            System.out.println("You must also provide an xsl file!\nUsage: java Converter <xml file name> <xsl file name>");
        else
        {
            try
            {
                //create a string variable and append the column names on the first line
                StringBuilder sb = new StringBuilder();
                sb.append("IP Address,Name,Port,Protocol,Service,Product,Version,Extra Info\n");

                //get necessary files and process them as necessary
                File xmlFile = new File(args[0]);
                File xslFile = new File(args[1]);
                File htmlFile = createHTML(xmlFile, xslFile);

                //parse the html so that text from different elements can be extracted
                org.jsoup.nodes.Document doc = Jsoup.parse(htmlFile, "UTF-8");

                //get all the IP addresses and the device name (if available) for those with open ports
                Elements names = doc.select("h2.up");
                for(TextNode node : names.textNodes())
                {
                    //ignore all h2 elements with no text inside
                    if(!node.isBlank())
                    {
                        //extract the ip address and the device name and
                        //get corresponding table of ports of the ip address
                        String[] info = node.text().split("/");
                        if(info.length > 1)
                            getTable(doc, sb, info[0].trim(), info[1].trim());
                        else
                            getTable(doc, sb, node.text().trim(), "");
                    }
                }

                //after all data has been finished processing, write all of that data to a csv file
                BufferedWriter writer = new BufferedWriter(new FileWriter("result.csv"));
                writer.write(sb.toString());
                writer.close();
            }

            catch(Exception e)
            {
                System.err.println("Sorry, there was a problem trying to convert your nmap xml file to csv!");
                e.printStackTrace();
            }
        }
    }


    /**
     * This method converts the XML file into an HTML file to be easily viewable in a browser.
     * @param xmlFile the xml file
     * @param xslFile the xml file's stylesheet
     * @return the html file
     * @throws Exception catch all errors and throw them to the main method
     */
    private static File createHTML(File xmlFile, File xslFile) throws Exception
    {
        File htmlFile = new File("nmap.html");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);

        StreamSource stylesource = new StreamSource(xslFile);
        Transformer transformer = TransformerFactory.newInstance().newTransformer(stylesource);
        Source source = new DOMSource(document);
        Result outputTarget = new StreamResult(htmlFile);
        transformer.transform(source, outputTarget);

        return htmlFile;
    }

    /**
     * This method converts a table of ports matching to the given ip address into a row in a CSV file.
     * @param doc the parsed html document object
     * @param sb the string variable
     * @param ip the ip address
     * @param name the device name (can be empty, not null)
     */
    private static void getTable(org.jsoup.nodes.Document doc, StringBuilder sb, String ip, String name)
    {
        //if there is no table of ports, report to console
        if(doc.select(String.format("table[id^=porttable_%s]", ip)).isEmpty())
            System.out.println("No table of ports found for: " + ip);

        else
        {
            //get table of ports
            Element table = doc.select(String.format("table[id^=porttable_%s]", ip)).get(0);

            //get only the open ports' information
            Elements rows = table.select("tr.open");

            //add the open port information to the csv data
            for(Element row : rows)
            {
                Elements cols = row.select("td");
                sb.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
                    ip, name, cols.get(0).text(), cols.get(1).text(),
                    cols.get(3).text().contains(",") ? "\"" + cols.get(3).text() + "\"" : cols.get(3).text(),
                    cols.get(5).text().contains(",") ? "\"" + cols.get(5).text() + "\"" : cols.get(5).text(),
                    cols.get(6).text().contains(",") ? "\"" + cols.get(6).text() + "\"" : cols.get(6).text(),
                    cols.get(7).text().contains(",") ? "\"" + cols.get(7).text() + "\"" : cols.get(7).text()
                ));
            }
        }
    }
}
