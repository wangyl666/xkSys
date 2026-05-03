package com.school.edu.service;

import com.school.edu.dto.QuestionDTO;
import com.school.edu.entity.Question;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileParserService {

    public List<QuestionDTO> parseFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new RuntimeException("文件名不能为空");
        }

        String text;
        if (filename.toLowerCase().endsWith(".pdf")) {
            text = extractTextFromPDF(file.getInputStream());
        } else if (filename.toLowerCase().endsWith(".docx")) {
            text = extractTextFromDocx(file.getInputStream());
        } else if (filename.toLowerCase().endsWith(".doc")) {
            text = extractTextFromDoc(file.getInputStream());
        } else if (filename.toLowerCase().endsWith(".txt")) {
            text = extractTextFromTxt(file.getInputStream());
        } else {
            throw new RuntimeException("不支持的文件格式，仅支持 PDF、Word(.doc/.docx) 和 TXT 文件");
        }

        return parseTextToQuestions(text);
    }

    private String extractTextFromPDF(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractTextFromDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractTextFromDoc(InputStream inputStream) throws IOException {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractTextFromTxt(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    public List<QuestionDTO> parseTextToQuestions(String text) {
        List<QuestionDTO> questions = new ArrayList<>();
        
        String normalizedText = text.replace("\r\n", "\n").replace("\r", "\n");
        
        Pattern questionPattern = Pattern.compile(
            "^(\\d+)[.、\\s]+(.+?)(?=(^\\d+[.、\\s]|$))",
            Pattern.MULTILINE | Pattern.DOTALL
        );
        
        Matcher matcher = questionPattern.matcher(normalizedText);
        List<String> questionBlocks = new ArrayList<>();
        
        while (matcher.find()) {
            questionBlocks.add(matcher.group(0).trim());
        }
        
        if (questionBlocks.isEmpty() && !normalizedText.trim().isEmpty()) {
            questionBlocks.add(normalizedText.trim());
        }
        
        for (String block : questionBlocks) {
            QuestionDTO question = parseQuestionBlock(block);
            if (question != null && question.getContent() != null && !question.getContent().isEmpty()) {
                questions.add(question);
            }
        }
        
        return questions;
    }

    private QuestionDTO parseQuestionBlock(String block) {
        String[] lines = block.split("\n");
        if (lines.length == 0) return null;

        QuestionDTO question = new QuestionDTO();
        question.setType(Question.QuestionType.SHORT_ANSWER);
        question.setScore(1.0);
        question.setIsPublic(false);

        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder optionsBuilder = new StringBuilder();
        String answer = null;
        boolean isOptionSection = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            Pattern answerPattern = Pattern.compile("^(?:参考)?答案[：:：]?\\s*(.+)$", Pattern.CASE_INSENSITIVE);
            Matcher answerMatcher = answerPattern.matcher(line);
            
            if (answerMatcher.find()) {
                answer = answerMatcher.group(1).trim();
                continue;
            }

            Pattern optionPattern = Pattern.compile("^([A-ZＡ-Ｚ])[.．、\\s]+(.+)$");
            Matcher optionMatcher = optionPattern.matcher(line);
            
            if (optionMatcher.find()) {
                isOptionSection = true;
                String optionLabel = optionMatcher.group(1);
                String optionContent = optionMatcher.group(2);
                
                optionLabel = optionLabel.replaceAll("[Ａ-Ｚ]", ch -> String.valueOf((char)(ch.charAt(0) - 'Ａ' + 'A')));
                
                if (optionsBuilder.length() > 0) {
                    optionsBuilder.append("\n");
                }
                optionsBuilder.append(optionLabel).append(". ").append(optionContent);
                continue;
            }

            Pattern questionNumPattern = Pattern.compile("^\\d+[.．、\\s]+(.+)$");
            Matcher questionNumMatcher = questionNumPattern.matcher(line);
            
            if (questionNumMatcher.find() && contentBuilder.length() == 0) {
                contentBuilder.append(questionNumMatcher.group(1).trim());
                continue;
            }

            if (!isOptionSection) {
                if (contentBuilder.length() > 0) {
                    contentBuilder.append(" ");
                }
                contentBuilder.append(line);
            }
        }

        question.setContent(contentBuilder.toString().trim());
        
        if (optionsBuilder.length() > 0) {
            question.setOptions(optionsBuilder.toString());
            question.setType(Question.QuestionType.SINGLE_CHOICE);
        }

        if (answer != null && !answer.isEmpty()) {
            question.setAnswer(answer);
            
            String upperAnswer = answer.toUpperCase();
            if (upperAnswer.length() <= 2 && upperAnswer.matches("[A-Z]+")) {
                if (upperAnswer.length() > 1) {
                    question.setType(Question.QuestionType.MULTIPLE_CHOICE);
                } else {
                    question.setType(Question.QuestionType.SINGLE_CHOICE);
                }
            } else if (answer.matches("(?i)^(正确|错误|对|错|true|false|是|否|√|×|T|F)$")) {
                question.setType(Question.QuestionType.TRUE_FALSE);
                if (answer.matches("(?i)^(正确|对|true|是|√|T)$")) {
                    question.setAnswer("正确");
                } else {
                    question.setAnswer("错误");
                }
            }
        }

        return question;
    }
}
