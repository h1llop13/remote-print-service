package com.remoteprint.telegram;

import com.remoteprint.config.TelegramProperties;
import com.remoteprint.document.DocumentValidationService;
import com.remoteprint.document.PageRangeService;
import com.remoteprint.document.PdfService;
import com.remoteprint.job.PrintJob;
import com.remoteprint.job.PrintJobService;
import com.remoteprint.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.io.FileInputStream;

@Component
public class RemotePrintTelegramBot
        implements LongPollingSingleThreadUpdateConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(RemotePrintTelegramBot.class);

    private final TelegramClient telegramClient;
    private final TelegramProperties telegramProperties;
    private final DocumentValidationService documentValidationService;
    private final PrintJobService printJobService;
    private final FileStorageService fileStorageService;
    private final PdfService pdfService;
    private final PageRangeService pageRangeService;

    public RemotePrintTelegramBot(
            TelegramProperties telegramProperties,
            DocumentValidationService documentValidationService,
            PrintJobService printJobService,
            FileStorageService fileStorageService,
            PdfService pdfService,
            PageRangeService pageRangeService
    ) {
        this.telegramProperties = telegramProperties;
        this.documentValidationService = documentValidationService;
        this.printJobService = printJobService;
        this.fileStorageService = fileStorageService;
        this.pdfService = pdfService;
        this.pageRangeService = pageRangeService;

        this.telegramClient =
                new OkHttpTelegramClient(
                        telegramProperties.getBotToken()
                );
    }

    @Override
    public void consume(Update update) {

        if (!update.hasMessage()) {
            return;
        }

        long chatId = update.getMessage().getChatId();

        if (!telegramProperties.getAllowedChatIdSet().contains(chatId)) {
            sendMessage(chatId, "Access denied.");
            return;
        }

        if (update.getMessage().hasText()) {

            String text = update.getMessage().getText();

            if ("/start".equals(text)) {
                sendMessage(
                        chatId,
                        "Remote Print Service is ready.\n\n"
                                + "Send me a PDF document to print."
                );
            }

            return;
        }

        if (update.getMessage().hasDocument()) {
            handleDocument(
                    chatId,
                    update.getMessage().getDocument()
            );
        }
    }

    private void handleDocument(
            long chatId,
            Document telegramDocument
    ) {

        try {
            String fileName = telegramDocument.getFileName();

            if (fileName == null
                    || !fileName.toLowerCase().endsWith(".pdf")) {

                sendMessage(
                        chatId,
                        "Only PDF files are supported."
                );

                return;
            }

            sendMessage(
                    chatId,
                    "Document received. Processing..."
            );

            GetFile getFile = GetFile.builder()
                    .fileId(telegramDocument.getFileId())
                    .build();

            org.telegram.telegrambots.meta.api.objects.File telegramFile =
                    telegramClient.execute(getFile);

            File downloadedFile =
                    telegramClient.downloadFile(
                            telegramFile.getFilePath()
                    );

            MockMultipartFile multipartFile;

            try (FileInputStream inputStream =
                         new FileInputStream(downloadedFile)) {

                multipartFile = new MockMultipartFile(
                        "file",
                        fileName,
                        "application/pdf",
                        inputStream
                );
            }

            documentValidationService.validatePdf(multipartFile);

            PrintJob job = printJobService.createJob(
                    fileName,
                    "application/pdf"
            );

            String originalFilePath =
                    fileStorageService.saveOriginalFile(
                            job.getId(),
                            multipartFile
                    );

            job.setOriginalFilePath(originalFilePath);

            int pageCount =
                    pdfService.getPageCount(originalFilePath);

            var selectedPages =
                    pageRangeService.parse(
                            "ALL",
                            pageCount
                    );

            job.setPageRange("ALL");

            String printableFilePath =
                    pdfService.createPrintablePdf(
                            originalFilePath,
                            selectedPages
                    );

            job.setPrintableFilePath(printableFilePath);

            PrintJob processedJob =
                    printJobService.processJob(job);

            if (processedJob.getStatus().name().equals("COMPLETED")) {

                sendMessage(
                        chatId,
                        "Print completed successfully."
                );

            } else {

                sendMessage(
                        chatId,
                        "Print failed.\n\n"
                                + processedJob.getErrorMessage()
                );
            }

        } catch (Exception exception) {

            log.error(
                    "Telegram document processing failed: chatId={}",
                    chatId,
                    exception
            );

            sendMessage(
                    chatId,
                    "Failed to process the document."
            );
        }
    }

    private void sendMessage(
            long chatId,
            String text
    ) {

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        try {
            telegramClient.execute(message);

        } catch (TelegramApiException exception) {

            log.error(
                    "Failed to send Telegram message to chatId={}",
                    chatId,
                    exception
            );
        }
    }
}