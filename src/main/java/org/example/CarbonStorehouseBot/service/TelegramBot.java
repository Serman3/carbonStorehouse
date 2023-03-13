package org.example.CarbonStorehouseBot.service;

import com.vdurmont.emoji.EmojiParser;
import org.example.CarbonStorehouseBot.config.BotConfig;
import org.example.CarbonStorehouseBot.model.*;
import org.example.CarbonStorehouseBot.repository.ColleagueRepository;
import org.example.CarbonStorehouseBot.repository.FabricRepository;
import org.example.CarbonStorehouseBot.repository.RollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
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
    private static final String HELP_TEXT = "Сохранять данные нужно в таком порядке:" + "\n" +
            "1) Когда начинаете новую партию, нажмите в меню кнопку <<addnewbatch>> после этого вводите " +
            "одним предложением через пробел, в порядке: название ткани, номер партии, метраж партии А60 23.012 400." +
            "2) После этого начинаете делать новый руллон, нажмите в меню кнопку <<addnewroll>>, вводите одним предложением через пробел, " +
            "в порядке: номер партии, номер руллона";
    private static final String YES_BUTTON = "YES_BUTTON";
    private static final String NO_BUTTON = "NO_BUTTON";
    private static final String START_OUTPUT = EmojiParser.parseToUnicode(" ,добрый день! " + "\n" + "Я бот, который поможет вам c работой по складскому учету" + " :blush:" + "\n" + "Выберети в меню действие."); // EmojiParser and :blush: это смайлик https://emojipedia.org/
    private static final String REGEX_ADD_NEW_BATCH = "[A|А][0-9]{2,3}\\s[0-9]{2,3}\\.[0-9]{2,3}\\s[0-9]{3,4}$";
    private static final String REGEX_DELETE_FABRIC = "[0-9]{2,3}\\.[0-9]{2,3}$";
    private static final String REGEX_ADD_NEW_ROLL = "[0-9]{2,3}\\.[0-9]{2,3}\\s\\d\\s[0-9]{2,3}$";
    private static final String REGEX_READY_FABRIC = "[0-9]{1,2}\\.[0-9]{2,3}\\sзавершить$";
    private static final String REGEX_REMARK_ROLL = "^\\d\\s[0-9]{2,3}\\.[0-9]{2,3}\\s[А-Яа-яЁё]*.*";
    private static final String REGEX_READY_ROLL = "^[0-9]{2,3}\\s\\d\\s[0-9]{2,3}\\.[0-9]{2,3}$";
    private Map<Long, String> inputDataUser = new HashMap<>();

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
        List<BotCommand> listOfCommands = new ArrayList<>();       // menu
        listOfCommands.add(new BotCommand("/start", "Вступительное сообщение"));
        listOfCommands.add(new BotCommand("/addnewbatch", "Начать новую партию"));
        listOfCommands.add(new BotCommand("/readyfabric", "Закончить партию"));
        listOfCommands.add(new BotCommand("/deletefabric", "Удалить партию"));
        listOfCommands.add(new BotCommand("/addnewroll", "Начать новый руллон"));
        listOfCommands.add(new BotCommand("/remarkroll", "Внести замечания по руллону"));
        listOfCommands.add(new BotCommand("/readyroll", "Закончить руллон"));
        listOfCommands.add(new BotCommand("/mydata", "Данные сотрудника"));
        listOfCommands.add(new BotCommand("/deletedata", "Удаление данных сотрудника"));
        listOfCommands.add(new BotCommand("/remainder", "Остаток по ткани"));
        listOfCommands.add(new BotCommand("/help", "Информация по боту"));
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
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()){
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();
            inputDataUser.put(chatId, messageText);

            if(messageText.equals("/start")){
                sendMessage(chatId, update.getMessage().getChat().getFirstName() + START_OUTPUT);
                saveColleague(update.getMessage());
            }

            //--------Работа с партией-------------
            if(messageText.equals("/addnewbatch")){
                //questionStatusFabric(chatId);
                sendMessage(chatId, "Для добавления новой партии ведите название ткани, номер партии, метраж партии одним предложением через пробел, в формате: А60 23.012 400");
            }
            if(inputDataUser.get(chatId).matches(REGEX_ADD_NEW_BATCH)){
                saveNewFabric(chatId, messageText);
            }
            if(messageText.equals("/deletefabric")){
                sendMessage(chatId, "Для удаления партии ведите номер партии, в формате: 23.023");
            }
            if(messageText.matches(REGEX_DELETE_FABRIC)){
                deleteFabric(chatId, messageText);
            }
            if(messageText.equals("/readyfabric")){
                sendMessage(chatId, "Чтобы законить партию введите номер партии и слово завершить одним предложеием через пробел, в формате: 23.023 завершить");
            }
            if(messageText.matches(REGEX_READY_FABRIC)){
                updateStatusFabric(chatId, messageText);
            }

            //--------Работа с руллоном-------------
            if(messageText.equals("/addnewroll")){
                sendMessage(chatId, "Для добавления руллона введите номер парти, номер руллона, метраж руллона одним предложением через пробел, в формате: 23.023 1 55");
            }
            if(messageText.matches(REGEX_ADD_NEW_ROLL)){
                saveNewRoll(messageText,chatId);
            }
            if(messageText.equals("/remarkroll")){
                sendMessage(chatId, "Для внесения замечания по руллону введите номер руллона, номер партии и текст замечания одним предложением через пробел, в формате: 1 23.023 текст замечания");
            }
            if(messageText.matches(REGEX_REMARK_ROLL)){
                updateRemarkRoll(chatId, messageText);
            }
            if(messageText.equals("/readyroll")){
                sendMessage(chatId, "Чтобы закончить руллон введите фактический метраж, номер руллона, номер партии одним предложеием через пробел, в формате: 55 1 23.023");
            }
            if(messageText.matches(REGEX_READY_ROLL)){
                updateStatusRoll(chatId, messageText);
            }

            //--------Работа с остатками-------------
            if(messageText.equals("/remainder")){
                SendMessage sendMessage = sendMessageFromFabricKeyboard(new SendMessage(String.valueOf(chatId), "Выберите по какой ткани показать остаток"));
                executeMessage(sendMessage);
            }
            if(messageText.equals("А160")){

            }




            //--------help-------------
            if(messageText.equals("/help")){
                sendMessage(chatId, HELP_TEXT);
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

    private void saveColleague(Message message){
        if(colleagueRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();
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
    }

    private void saveNewRoll(String inputData, long chatId){
        String[] data = inputData.split("\s");
        if(data.length != 3){
            sendMessage(chatId, "Не удается добавить руллон, вы ввели не все данные");
        }else {
            Fabric fabric = fabricRepository.findById(data[0]).orElse(null);
            if(fabric != null){
                Roll roll = new Roll();
                roll.setNumberRoll(Integer.parseInt(data[1]));
                roll.setRollMetric(Integer.parseInt(data[2]));
                roll.setDateFulfilment(LocalDateTime.now());
                roll.setStatusRoll(StatusRoll.AT_WORK);
                roll.setRemark(null);
                roll.setFabric(fabric);
                rollRepository.save(roll);
                sendMessage(chatId, "Руллон под номером " + data[1] + " добавлен к партии " + data[0]);
            }else {
                sendMessage(chatId, "Не удается добавить руллон, вы ввели несуществующий номер партии");
            }
        }
    }

    private void saveNewFabric(long chatId, String inputData){
        String[] data = inputData.split("\s");
        if(fabricRepository.findById(data[1]).isEmpty()){
            Colleague colleague = colleagueRepository.findById(chatId).get();
            Fabric fabric = new Fabric();
            fabric.setBatchNumberId(data[1]);
            fabric.setDateManufacture(LocalDateTime.now());
            fabric.setMetricAreaBatch(Integer.parseInt(data[2]));
            fabric.setNameFabric(data[0]);
            fabric.addColleague(colleague);
            fabric.setStatusFabric(StatusFabric.AT_WORK);
            fabricRepository.save(fabric);
            System.out.println(colleague);
            sendMessage(chatId, "Вы начали делать новую партию " + inputData);
        }else{
            sendMessage(chatId, "Такая партия уже сущесвует");
        }
    }

    @Transactional
    private void deleteFabric(long chatId, String fabricId){
        Fabric fabric = fabricRepository.findById(fabricId).orElse(null);
        if (fabric != null){
            rollRepository.deleteAllByFabricId(fabric.getBatchNumberId());
            fabric.remove();
            fabricRepository.deleteFabric(fabric.getBatchNumberId());
            sendMessage(chatId, "Партия с номером " + fabricId + " была удалена");
        }
        else {
            sendMessage(chatId, "Партии с номером " + fabricId + " нет в базе данных для удаления");
        }
    }

    private void updateRemarkRoll(long chatId, String inputData){
        String[] data = inputData.split("\s");
        if(data.length < 2){
            sendMessage(chatId, "Не удается внести изменения по руллону, вы ввели не все данные");
        }
        String numberRollAndNumberFabric = data[0] + " " + data[1] + " ";
        String remark = inputData.replaceFirst(numberRollAndNumberFabric, "");
        Fabric fabric = fabricRepository.findById(data[1]).orElse(null);
        if(fabric != null){
            Roll roll = rollRepository.findByNumberRollAndFabricId(Integer.parseInt(data[0]), data[1]).get();
            roll.setRemark(remark);
            roll.setDateFulfilment(LocalDateTime.now());
            rollRepository.save(roll);
            sendMessage(chatId, "Замечания по руллону " + data[0] + " в партии " + data[1] + " внесены");
        }else{
            sendMessage(chatId, "Партии с номером " + data[1] + " нет в базе данных для внесения замечания по руллону");
        }
    }

    private void updateStatusRoll(long chatId, String inputData){
        String[] data = inputData.split("\s");
        if(data.length != 3){
            sendMessage(chatId, "Не удается завершить руллон, вы ввели не все данные");
        }
        Roll roll = rollRepository.findByNumberRollAndFabricId(Integer.parseInt(data[1]), data[2]).orElse(null);
        if(roll != null){
            roll.setStatusRoll(StatusRoll.READY);
            roll.setRollMetric(Integer.parseInt(data[0]));
            roll.setDateFulfilment(LocalDateTime.now());
            rollRepository.save(roll);
            sendMessage(chatId, "Руллон с номером " + data[1] + " завершен");
        }else{
            sendMessage(chatId, "Руллона с номером " + data[1] + " нет в базе данных для завершения");
        }
    }

    private void updateStatusFabric(long chatId, String inputData){
        String[] data = inputData.split("\s");
        if(data.length != 2){
            sendMessage(chatId, "Не удается завершить партию, вы ввели не все данные");
        }
        Fabric fabric = fabricRepository.findById(data[0]).orElse(null);
        if(fabric != null){
            fabric.setStatusFabric(StatusFabric.READY);
            fabric.setDateManufacture(LocalDateTime.now());
            fabricRepository.save(fabric);
            sendMessage(chatId, "Партия с номером " + data[0] + " завершена");
        }else{
            sendMessage(chatId, "Партии с номером " + data[0] + " нет в базе данных для завершения");
        }
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        executeMessage(sendMessage);
    }

    public SendMessage sendMessageFromFabricKeyboard(SendMessage sendMessage){
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
        return sendMessage;
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
