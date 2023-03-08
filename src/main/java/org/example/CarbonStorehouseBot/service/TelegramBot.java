package org.example.CarbonStorehouseBot.service;

import com.vdurmont.emoji.EmojiParser;
import org.example.CarbonStorehouseBot.config.BotConfig;
import org.example.CarbonStorehouseBot.model.Colleague;
import org.example.CarbonStorehouseBot.model.Fabric;
import org.example.CarbonStorehouseBot.model.NameFabrics;
import org.example.CarbonStorehouseBot.model.Roll;
import org.example.CarbonStorehouseBot.repository.ColleagueRepository;
import org.example.CarbonStorehouseBot.repository.FabricRepository;
import org.example.CarbonStorehouseBot.repository.RollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private ColleagueRepository colleagueRepository;
    @Autowired
    private RollRepository rollRepository;
    @Autowired
    private FabricRepository fabricRepository;
    private final BotConfig botConfig;
    private static final String HELP_TEXT = "help text";
    private static final String YES_BUTTON = "YES_BUTTON";
    private static final String NO_BUTTON = "NO_BUTTON";
    private static final String ERROR_TEXT = "Error occurred: ";
    private static final String INPUT_DATA_TO_FABRIC = "Введите данные новой партии одним предложением через пробел, в порядке: название ткани, номер партии, метраж партии (А60 23.012 400)";
    private List<Long> listOfUser = new ArrayList<>();
    private Map<Long, List<String>> mapAnswer = new HashMap<>();

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
        List<BotCommand> listOfCommands = new ArrayList<>();       // menu
        listOfCommands.add(new BotCommand("/start", "Вступительное сообщение"));
        listOfCommands.add(new BotCommand("/enterfabricandroll", "Внести данные по ткани и руллону"));
        listOfCommands.add(new BotCommand("/mydata", "Данные сотрудника"));
        listOfCommands.add(new BotCommand("/deletedata", "удаление данных сотрудника"));
        listOfCommands.add(new BotCommand("/help", "Информация по боту"));
        listOfCommands.add(new BotCommand("/remainder", "Остаток по ткани"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }catch (TelegramApiException e){
            e.printStackTrace();
            //log.error("Error setting bot command list " + e.getMessage());
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
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();

            switch (messageText){
                case "/start":
                    startCommand(chatId, update.getMessage().getChat().getFirstName(), messageText);
                    registeredColleague(update.getMessage());
                break;

                case "/enterfabricandroll":
                    if(!listOfUser.contains(chatId)){
                        listOfUser.add(chatId);
                        questionStatusFabric(chatId);
                        sendMessage(chatId, INPUT_DATA_TO_FABRIC, messageText);
                    }else {
                        List<String> list = new ArrayList<>();
                        list.add(messageText);
                        mapAnswer.put(chatId, list);
                        listOfUser.remove(chatId);
                        enterFabricAndRollData(chatId, mapAnswer.get(chatId));
                        mapAnswer.remove(chatId);
                    }
                    break;

                case "/remainder":
                    sendMessage(chatId, "Покакой ткани показать остаток?", messageText);

                    break;

                case "/help":
                    sendMessage(chatId, HELP_TEXT, messageText);
                    break;

                default: sendMessage(chatId, "Такой фичи еще нет", messageText);
            }
        }else if(update.hasCallbackQuery()){
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if(callbackData.equals(YES_BUTTON)){
                String text = "Создаем новую партию";
                executeEditMessageText(text, chatId, messageId);
            }
            else if(callbackData.equals(NO_BUTTON)){
                String text = "Рабботаем с имеющейся на складе партией";
                executeEditMessageText(text, chatId, messageId);
            }
        }
    }

    private void registeredColleague(Message message){
        if(colleagueRepository.findById(message.getChatId()).isEmpty()){
            var chatId = message.getChatId();
            var chat = message.getChat();
            saveColleague(chatId, chat);
        }
    }

    private void startCommand(long chatId, String nameUser, String messageText) {
        String outputMessage = EmojiParser.parseToUnicode("Добрый день! " + nameUser + "\n" + "Я бот, который поможет вам c работой по складскому учету." + " :blush:"); // EmojiParser and :blush: это смайлик https://emojipedia.org/
        sendMessage(chatId, outputMessage, messageText);
        System.out.println("UserName " + nameUser);
        //log.info("UserName " + nameUser);
    }

    private void sendMessage(long chatId, String textToSend, String messageText) {   // метод отправляет сообщения в тг пользователю
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        if(messageText.equals("/remainder")){
            sendMessageFromFabricKeyboard(sendMessage);
        }
        executeMessage(sendMessage);
    }

    private void saveColleague(Long chatId, Chat chat){
        Colleague colleague = new Colleague();
        colleague.setChatId(chatId);
        colleague.setFirstName(chat.getFirstName());
        colleague.setLastName(chat.getLastName());
        colleague.setUserName(chat.getUserName());
        colleague.setDateRegister(LocalDateTime.now());
        colleagueRepository.save(colleague);
        System.out.println("Colleague save " + colleague);
        //log.info("Colleague save " + colleague);
    }

    private void enterFabricAndRollData(long chatId, List<String> inputData){
        Colleague colleague = colleagueRepository.findById(chatId).get();

        Roll roll = new Roll();
        roll.setNumberRoll(1);
        roll.setRollMetric(55);
        roll.setRemark("test");
        roll.setDateFulfilment(LocalDateTime.now());
        rollRepository.save(roll);

        Fabric fabric = new Fabric();
        fabric.setBatchNumber("23.010");
        fabric.setDateManufacture(LocalDateTime.now());
        fabric.setMetricAreaBatch(200);
        fabric.setNameFabric(NameFabrics.A60);
        fabric.addColleague(colleague);
        fabric.addRoll(roll);
        fabricRepository.save(fabric);
    }

    public void sendMessageFromFabricKeyboard(SendMessage sendMessage){
        //клавиатура привязывается к конеретному сообщению
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(NameFabrics.A40.toString());
        row.add(NameFabrics.A60.toString());
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add(NameFabrics.A80.toString());
        row.add(NameFabrics.A120.toString());
        row.add(NameFabrics.A160.toString());
        keyboardRows.add(row);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
    }

    private void executeEditMessageText(String text, long chatId, long messageId){
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            //log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message){
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            //log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void questionStatusFabric(long chatId){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("Новая партия?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();

        yesButton.setText("Да");
        yesButton.setCallbackData(YES_BUTTON);

        var noButton = new InlineKeyboardButton();

        noButton.setText("Нет");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        sendMessage.setReplyMarkup(markupInLine);

        executeMessage(sendMessage);
    }

}
