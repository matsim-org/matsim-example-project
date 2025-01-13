package org.matsim.project;

import com.ctc.wstx.exc.WstxOutputException;

import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class XmlParserTestChargerChange {

    public static void main(String[] args) throws XMLStreamException, FileNotFoundException {
        String path = "C:\\Users\\ylvlo\\Documents\\Examensarbete\\Kod\\matsim-example-project2\\test\\input\\org\\matsim\\evDetour\\1pctNetwork.xml";

        ArrayList<StartElement> links = XmlParserTestChargerChange.findTagsInXMLFile(path, "link");
        String[] linkIDs = new String[links.size()];
        for(int i = 0; i < links.size(); i++){
            linkIDs[i] = XmlParserTestChargerChange.getValueFromTag(links.get(i), "id");
            System.out.println(linkIDs[i]);
        }

        Random rand = new Random();
        int workIdIndex = 0;
        int homeIdIndex = 0;
        int chargerIdIndex = 0;
        while(workIdIndex == homeIdIndex || chargerIdIndex == homeIdIndex  || workIdIndex == chargerIdIndex){
            workIdIndex = rand.nextInt(linkIDs.length);
            homeIdIndex = rand.nextInt(linkIDs.length);
            chargerIdIndex = rand.nextInt(linkIDs.length);
        }
        String newWorkId = linkIDs[workIdIndex];
        String newHomeId = linkIDs[homeIdIndex];
        String newChargerId = linkIDs[chargerIdIndex];
        String newEventType = "charging";


        String plansPath = "C:\\Users\\ylvlo\\Documents\\Examensarbete\\Kod\\matsim-example-project2\\test\\input\\org\\matsim\\evDetour\\triple-charger-plan.xml";
        String outputTempPath = "C:\\Users\\ylvlo\\Documents\\Examensarbete\\Kod\\matsim-example-project2\\test\\input\\org\\matsim\\evDetour\\triple-charger-plan-test.xml";
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

        XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileInputStream(plansPath));
        XMLEventWriter writer = xmlOutputFactory.createXMLEventWriter(new FileOutputStream(outputTempPath));

        int i = 0;
        while(reader.hasNext()){
            //System.out.println("Gets here");
            XMLEvent nextEvent = reader.nextEvent();
            if(nextEvent.isStartElement()) {
                StartElement startElement = nextEvent.asStartElement();
                if (startElement.getName().getLocalPart().equals("activity")) {
                    Iterator<Attribute> attributes = startElement.getAttributes();
                    while(attributes.hasNext()){
                        Attribute at = attributes.next();
                        if(at.getName().getLocalPart().equals("type")){
                            if (at.getValue().equals("work") && i < 1){
                                StartElement newST = modifyAndReturnStartElement(startElement, "type", newEventType);
                                writer.add(modifyAndReturnStartElement(newST,"link", newChargerId));
                                i++;
                            } else if(at.getValue().equals("work")){
                                writer.add(modifyAndReturnStartElement(startElement, "link", newWorkId));
                            } else if (at.getValue().equals("home")) {
                                writer.add(modifyAndReturnStartElement(startElement, "link", newHomeId));
                            }
                            break;
                        }
                    }
                } else{
                    writer.add(startElement);
                }
            } else {
                writer.add(nextEvent);
            }
        }

        reader.close();
        writer.close();

    }

    public static ArrayList<StartElement> findTagsInXMLFile(String filePath, String tagName) throws FileNotFoundException, XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileInputStream(filePath));

        ArrayList<StartElement> foundTags = new ArrayList<>();
        while(reader.hasNext()){
            XMLEvent nextEvent = reader.nextEvent();
            if (nextEvent.isStartElement()) {
                StartElement startElement = nextEvent.asStartElement();
                if(startElement.getName().getLocalPart().equals(tagName)){
                    foundTags.add(startElement);
                }
            }
        }
        return foundTags;
    }

    public static String getValueFromTag(StartElement tag, String valName){
        Iterator<Attribute> attributes = tag.getAttributes();
        while(attributes.hasNext()){
            Attribute at = attributes.next();
            if(at.getName().getLocalPart().equals(valName)){
                return at.getValue();
            }

        }
        return "not found";

    }

    private static StartElement modifyAndReturnStartElement(StartElement startElement, String attributeToEditName, String newValue){

        Iterator<Attribute> attributes = startElement.getAttributes();
        ArrayList<Attribute> updatedAttributes = new ArrayList<>();

        XMLEventFactory eventFactory = XMLEventFactory.newInstance();

        while(attributes.hasNext()){
            Attribute at = attributes.next();
            if(at.getName().getLocalPart().equals(attributeToEditName)){
                updatedAttributes.add(eventFactory.createAttribute(attributeToEditName, newValue));
            } else {
                updatedAttributes.add(at);
            }
        }

        return eventFactory.createStartElement(startElement.getName(), updatedAttributes.iterator(), startElement.getNamespaces());
    }

}
