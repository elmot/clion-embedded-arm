package xyz.elmot.clion.openocd.test;

import java.io.IOException;
import java.io.StringReader;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;
import xyz.elmot.clion.openocd.OpenOcdConfiguration;

public class OpenOcdConfigurationTest extends Assert {
    private static final String XMLNS = OpenOcdConfiguration.NAMESPACE.getPrefix();
    private static final String XMLNS_URI = OpenOcdConfiguration.NAMESPACE.getURI();

    private static Element toXmlElement(String attrValuePairs) throws JDOMException {
        SAXBuilder builder = new SAXBuilder();
        String xmlDocStr = "<?xml version=\"1.0\"?><testdocument xmlns:" + XMLNS + "=\"" +  XMLNS_URI + "\">" +
                "<testelement" + attrValuePairs + "/></testdocument>";

        Document doc = null;
        try {
            doc = builder.build(new StringReader(xmlDocStr));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc.getRootElement().getChild("testelement");
    }

    private static String xmlAttrValue(String attr, String value) {
        return " " + XMLNS + ":" + attr +"=\"" + value + "\" ";
    }

    private static <T> boolean ensureAllSameType(T[] values) {
        Class cls = null;
        boolean result = true;
        for (T item : values) {
            if (cls == null) {
                cls = item.getClass();
            } else {
                result = result && cls.equals(item.getClass());
            }
        }
        return result;
    }

    enum RegularEnum {
        ONE(1),
        TWO(2),
        THREE(3),
        FOUR(4);

        private final int value;
        RegularEnum(int v) {
            value = v;
        }
        int getValue() { return value; }
    };

    enum EnumWithItemsOfUniqueAnonymousSubtypeEach {
        ONE {
            int getItemData() {
                return 1;
            }
        },
        TWO {
            int getItemData() {
                return 2;
            }
        },
        THREE {
            int getItemData() {
                return 3;
            }
        },
        FOUR {
            int getItemData() {
                return 4;
            }
        };

        abstract int getItemData();
    };

    @Test
    public void ensureAllSameTypeTest() {
        assertTrue("All regular enum items have same type",
                ensureAllSameType(RegularEnum.values()));
        assertFalse("Subclassed enum items have different types",
                ensureAllSameType(EnumWithItemsOfUniqueAnonymousSubtypeEach.values()));
    }

    @Test
    public void openOcdEnumsTest() {
        assertTrue("Ensure ResetType enum is regular enum",
                ensureAllSameType(OpenOcdConfiguration.ResetType.values()));
        assertTrue("Ensure UploadType enum is regular enum",
                ensureAllSameType(OpenOcdConfiguration.UploadType.values()));
    }

    @Test
    public void readIntAttrTest() throws JDOMException {
        Element element = toXmlElement(
                xmlAttrValue("attr1", "5") +
                xmlAttrValue("attr2", "text")
        );
        int attr1 = OpenOcdConfiguration.readIntAttr(element, "attr1", 2);
        int attr2 = OpenOcdConfiguration.readIntAttr(element, "attr2", 15);
        int attr3 = OpenOcdConfiguration.readIntAttr(element, "attr3", 29);
        assertEquals("Can read valid attribute", 5, attr1);
        assertEquals("Can return default value for non-integer attribute", 15, attr2);
        assertEquals("Can return default value for missing attribute", 29, attr3);
    }
    @Test
    public void readStringAttrTest() throws JDOMException {
        Element element = toXmlElement(
                xmlAttrValue("attr1", "qwerty")
        );
        String attr1 = OpenOcdConfiguration.readStringAttr(element, "attr1", "undefined");
        String attr2 = OpenOcdConfiguration.readStringAttr(element, "attr2", "undefined");
        assertEquals("Can read valid attribute", "qwerty", attr1);
        assertEquals("Can return default value for missing attribute", "undefined", attr2);
    }
    @Test
    public void readResetTypeEnumAttrTest() throws JDOMException {
        final String attrName1 = "resetattr1";
        final String attrName2 = "resetattr2";
        final String attrName3 = "resetattr3";
        Element uploadElement = toXmlElement(
                xmlAttrValue(attrName1, "HALT") +
                        xmlAttrValue(attrName2, "INVALID_VALUE")
        );
        OpenOcdConfiguration.ResetType resetTypeHalt =
                OpenOcdConfiguration.readEnumAttr(uploadElement, attrName1, OpenOcdConfiguration.ResetType.INIT);
        OpenOcdConfiguration.ResetType resetTypeMissing2 =
                OpenOcdConfiguration.readEnumAttr(uploadElement, attrName2, OpenOcdConfiguration.ResetType.NONE);
        OpenOcdConfiguration.ResetType resetTypeMissing3 =
                OpenOcdConfiguration.readEnumAttr(uploadElement, attrName3, OpenOcdConfiguration.ResetType.NONE);
        assertEquals("Can read valid attribute", resetTypeHalt, OpenOcdConfiguration.ResetType.HALT);
        assertEquals("Can return default value for invalid attribute", resetTypeMissing2,
                OpenOcdConfiguration.ResetType.NONE);
        assertEquals("Can return default value for missing attribute", resetTypeMissing3,
                OpenOcdConfiguration.ResetType.NONE);
    }
    @Test
    public void readUploadTypeEnumAttrTest() throws JDOMException {
        final String attrName1 = "uploadattr1";
        final String attrName2 = "uploadattr2";
        final String attrName3 = "uploadattr3";
        Element uploadElement = toXmlElement(
                xmlAttrValue(attrName1, "UPDATED_ONLY") +
                        xmlAttrValue(attrName2, "INVALID_VALUE")
        );
        OpenOcdConfiguration.UploadType uploadTypeUpdatedOnly =
                OpenOcdConfiguration.readEnumAttr(uploadElement, attrName1, OpenOcdConfiguration.UploadType.ALWAYS);
        OpenOcdConfiguration.UploadType uploadTypeMissing2 =
                OpenOcdConfiguration.readEnumAttr(uploadElement, attrName2, OpenOcdConfiguration.UploadType.NONE);
        OpenOcdConfiguration.UploadType uploadTypeMissing3 =
                OpenOcdConfiguration.readEnumAttr(uploadElement, attrName3, OpenOcdConfiguration.UploadType.NONE);
        assertEquals("Can read valid attribute", uploadTypeUpdatedOnly,
                OpenOcdConfiguration.UploadType.UPDATED_ONLY);
        assertEquals("Can return default value for invalid attribute", uploadTypeMissing2,
                OpenOcdConfiguration.UploadType.NONE);
        assertEquals("Can return default value for missing attribute", uploadTypeMissing3,
                OpenOcdConfiguration.UploadType.NONE);
    }
}
