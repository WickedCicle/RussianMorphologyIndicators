package ru.mai;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianMorphology;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static void main(String[] args) throws IOException {

        LuceneMorphology luceneMorph = new RussianLuceneMorphology();

        ArrayList<String> texts = new ArrayList<>();

        Files.walk(Paths.get("D:\\Downloads\\RNC_million\\RNC_million\\sample_ar\\TEXTS"))
                .filter(Files::isRegularFile)
                .forEach((file -> {
                    texts.add(file.toString());
                }));

        AtomicInteger intUnfamilliar = new AtomicInteger();
        AtomicInteger intKnown = new AtomicInteger();
        AtomicInteger wordCount = new AtomicInteger();
        AtomicInteger accuracy = new AtomicInteger();
        AtomicBoolean isAdded = new AtomicBoolean(false);

        Instant start;
        Instant finish;
        long elapsed = 0;

        //System.out.println(luceneMorph.getNormalForms("деревья"));
        //System.out.println(luceneMorph.getMorphInfo("деревья"));

        try {
            for (String text : texts) {
                System.out.println("next file" + text);
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = documentBuilder.parse(text);

                Node html = document.getDocumentElement();

                NodeList htmlProps = html.getChildNodes();
                for (int i = 0; i < htmlProps.getLength(); i++) {
                    Node body = htmlProps.item(i);
                    if (body.getNodeType() != Node.TEXT_NODE && body.getNodeName().equals("body")) {
                        NodeList bodyProps = body.getChildNodes();
                        for (int j = 0; j < bodyProps.getLength(); j++) {
                            Node paragraph = bodyProps.item(j);
                            if (paragraph.getNodeType() != Node.TEXT_NODE && (paragraph.getNodeName().equals("p") || paragraph.getNodeName().equals("speach"))) {
                                NodeList paragraphProps = paragraph.getChildNodes();
                                for (int k = 0; k < paragraphProps.getLength(); k++) {
                                    Node sentence = paragraphProps.item(k);
                                    if (sentence.getNodeType() != Node.TEXT_NODE && sentence.getNodeName().equals("se")) {
                                        NodeList sentenceProps = sentence.getChildNodes();
                                        for (int m = 0; m < sentenceProps.getLength(); m++) {
                                            Node word = sentenceProps.item(m);
                                            if (word.getNodeType() != Node.TEXT_NODE && word.getNodeName().equals("w")) {
                                                wordCount.getAndIncrement();
                                                NodeList wordProps = word.getChildNodes();
                                                start = Instant.now();;
                                                for (int n = 0; n < wordProps.getLength(); n++) {
                                                    Node characteristics = wordProps.item(n);
                                                    if (isAdded.get()) {
                                                        continue;
                                                    }

                                                    if (luceneMorph.checkString(word.getTextContent().toLowerCase(Locale.ROOT).replaceAll("[` ]", ""))){
                                                        intKnown.getAndIncrement();
                                                        isAdded.set(true);
                                                        if (Objects.equals(luceneMorph.getNormalForms(word.getTextContent().toLowerCase(Locale.ROOT).replaceAll("[` ]", "")).get(0), characteristics.getAttributes().getNamedItem("lex").getNodeValue().toLowerCase(Locale.ROOT).replaceAll("ё", "е"))){
                                                            accuracy.getAndIncrement();
                                                        }
                                                    } else {
                                                        intUnfamilliar.getAndIncrement();
                                                        isAdded.set(true);
                                                    }
                                                }
                                                finish = Instant.now();
                                                elapsed += Duration.between(start, finish).toMillis();
                                                isAdded.set(false);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            System.out.println("Количество ненайдённых: " + intUnfamilliar);
            System.out.println("Количество найдённых в словаре: " + intKnown);
            System.out.println("Общее количество слов: " + wordCount);
            System.out.println("Точно определенных начальных форм слов: " + accuracy);
            System.out.println("Процент ненайдённых:" + intUnfamilliar.doubleValue()/wordCount.doubleValue());
            System.out.println("Точность: " + accuracy.doubleValue()/intKnown.doubleValue());
            System.out.println("Затраченное время: " + (double)elapsed/1000 + " секунд");


        } catch (ParserConfigurationException | SAXException | IOException ex) {
            ex.printStackTrace(System.out);
        }
    }
}
