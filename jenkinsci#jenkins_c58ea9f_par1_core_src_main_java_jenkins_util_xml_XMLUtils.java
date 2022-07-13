package jenkins.util.xml;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;

/**
 * Utilities useful when working with various XML types.
 */
public final class XMLUtils {

    private final static Logger LOGGER = LogManager.getLogManager().getLogger(XMLUtils.class.getName());

    /**
     * Transform the source to the output in a manner that is protected against XXE attacks.
     * If the transform can not be completed safely then an IOException is thrown.
     * Note - to turn off safety set the system property <code>disableXXEPrevention</code> to <code>true</code>.
     * @param source The XML input to transform. - This should be a <code>StreamSource</code> or a
     *               <code>SAXSource</code> in order to be able to prevent XXE attacks.
     * @param out The Result of transforming the <code>source</code>.
     */
    public static void safeTransform(@Nonnull Source source, @Nonnull Result out) throws TransformerException,
            SAXException {

        InputSource src = SAXSource.sourceToInputSource(source);
        if (src != null) {
            SAXTransformerFactory stFactory = (SAXTransformerFactory) TransformerFactory.newInstance();
            stFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            // defend against XXE
            xmlReader.setEntityResolver(RestrictiveEntityResolver.INSTANCE);
            SAXSource saxSource = new SAXSource(xmlReader, src);
            _transform(saxSource, out);
        }
        else {
            // for some reason we could not convert source
            // this applies to DOMSource and StAXSource - and possibly 3rd party implementations...
            // a DOMSource can already be compromised as it is parsed by the time it gets to us.
            if (Boolean.getBoolean("disableXXEPrevention")) {
                LOGGER.warning("Parsing XML with XXEPrevention disabled!");
                _transform(source, out);
            }
            else {
                throw new TransformerException("Could not convert source of type " + source.getClass() + " and " +
                        "XXEPrevention is enabled.");
            }
        }
    }

    /**
     * potentially unsafe XML transformation.
     * @param source The XML input to transform.
     * @param out The Result of transforming the <code>source</code>.
     */
    private static void _transform(Source source, Result out) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        // this allows us to use UTF-8 for storing data,
        // plus it checks any well-formedness issue in the submitted data.
        Transformer t = factory.newTransformer();
        t.transform(source, out);
    }

}