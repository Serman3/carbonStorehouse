package org.example.CarbonStorehouseBot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.example.CarbonStorehouseBot.config.BotConfig;
import org.example.CarbonStorehouseBot.model.Colleague;
import org.example.CarbonStorehouseBot.repository.ColleagueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private ColleagueRepository colleagueRepository;
    private final BotConfig botConfig;
    private final static String HELP_TEXT = "";

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
        List<BotCommand> listOfCommands = new ArrayList<>();       // кнопки
        listOfCommands.add(new BotCommand("/start", "Вступительное сообщение"));
        listOfCommands.add(new BotCommand("/mydata", "Данные сотрудника"));
        listOfCommands.add(new BotCommand("/deletedata", "удаление данных сотрудника"));
        listOfCommands.add(new BotCommand("/help", "Информация по боту"));
        listOfCommands.add(new BotCommand("/settings", "Настройки"));
        listOfCommands.add(new BotCommand("/remainder", "Остаток по ткани"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }catch (TelegramApiException e){
            log.error("Error setting bot command list " + e.getMessage());
        }

    }
    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {      // главный метод, который обрабатывает все входящие сообщения
        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText){
                case "/start":
                    startCommand(chatId, update.getMessage().getChat().getFirstName(), messageText);
                    registeredColleague(update.getMessage());
                break;

                case "/remainder":
                    sendMessage(chatId, "По какой ткани показать остаток?", messageText);

                case "/help":
                    sendMessage(chatId, HELP_TEXT, messageText);
                    break;

                default: sendMessage(chatId, "Такой фичи еще нет", messageText);
            }
        }
    }

    private void registeredColleague(Message message){
        if(colleagueRepository.findById(message.getChatId()).isEmpty()){
            var chatId = message.getChatId();
            var chat = message.getChat();

            Colleague colleague = new Colleague();
            colleague.setChatId(chatId);
            colleague.setFirstName(chat.getFirstName());
            colleague.setLastName(chat.getLastName());
            colleague.setUserName(chat.getUserName());
            colleague.setStatus_time(LocalDateTime.now());

            colleagueRepository.save(colleague);
            log.info("Colleague save " + colleague);

        }
    }

    private void startCommand(long chatId, String nameUser, String messageText) {
        //String outputMessage = "Добрый день! " + nameUser + "\n" + "Я бот, который поможет вам c работой по складскому учету.";
        String outputMessage = EmojiParser.parseToUnicode("Добрый день! " + nameUser + "\n" + "Я бот, который поможет вам c работой по складскому учету." + " :blush:"); // EmpjiParser and :blush: это смайлик https://emojipedia.org/
        sendMessage(chatId,outputMessage, messageText);
        log.info("UserName " + nameUser);
    }

    private void sendMessage(long chatId, String textToSend, String messageText) {   // метод отправляет сообщения в тг пользователю
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        if(messageText.equals("/remainder")){
            sendMessageFromFabricKeyboard(sendMessage);
        }
        try{
            execute(sendMessage);
        }catch (TelegramApiException e){
            log.error("Error occurred " + e.getMessage());
        }
    }

    public void sendMessageFromFabricKeyboard(SendMessage sendMessage){
        //клавиатура привязывается к конеретному сообщению
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        row.add("A40");
        row.add("A60");
        keyboardRows.add(row);

        row = new KeyboardRow();

        row.add("A80");
        row.add("A120");
        row.add("A160");
        keyboardRows.add(row);

        replyKeyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
    }

}
