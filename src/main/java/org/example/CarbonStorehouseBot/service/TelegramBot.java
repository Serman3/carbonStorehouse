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
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.File;
import java.time.LocalDate;
import java.util.*;


//@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private ColleagueRepository colleagueRepository;
    @Autowired
    private RollRepository rollRepository;
    @Autowired
    private FabricRepository fabricRepository;
    @Autowired
    private WriteFile writeFile;
    private final BotConfig botConfig;
    private static final String YES_BUTTON = "YES_BUTTON";
    private static final String NO_BUTTON = "NO_BUTTON";
    private Map<Long, String> userStates = new HashMap<>();
    private static final double widthRoll = 0.92;
    private static final double[] widthsThread = {0.020, 0.022, 0.027};
    private static final String[] periodDate = {"Текущий месяц", "За 2 месяца", "За полгода", "Текущий год"};
    private static final String START_OUTPUT = EmojiParser.parseToUnicode(", добрый день! " + "\n" + "Я бот, который поможет вам c работой по складскому учету" + " :blush:" + "\n" + "Выберети в меню действие."); // EmojiParser and :blush: это смайлик https://emojipedia.org/
    private static final String REGEX_ADD_NEW_FABRIC = "[A|А][0-9]{2,3}\\s[0-9]{2,3}\\.[0-9]{2,3}\\s[0-9]{3,4}$";
    private static final String REGEX_NUMBER_FABRIC_AND_ROLL = "^[0-9]{2,3}\\.[0-9]{2,3}\\s\\d$";
    private static final String REGEX_REMARK_ROLL = "^[0-9]{2,3}\\.[0-9]{2,3}\\s\\d\\s[А-Яа-яЁё]*.*";
    private static final String REGEX_UPDATE_METRIC_ROLL = "^[0-9]{2,3}\\.[0-9]{2,3}\\s\\d\\s[0-9]{1,3}\\.[0-9]{1,3}$";
    private static final String REGEX_NUMBER_FABRIC_AND_ROLL_AND_METRIC = "^[0-9]{2,3}\\.[0-9]{2,3}\\s\\d\\s[0-9]{1,2}$";
    private static final String REGEX_NUMBER_FABRIC_AND_ROLL_AND_STEP = "^[0-9]{2,3}\\.[0-9]{2,3}\\s\\d\\s[0-9]{2,4}$";
    private static final String REGEX_READY_FABRIC_DATA = "^[0-9]{2,3}\\.[0-9]{2,3}\\s\\А[0-9]{2,3}$";
    private static final String REGEX_NUMBER_FABRIC = "^[0-9]{2,3}\\.[0-9]{2,3}$";
    private static final String HELP_TEXT = "Чтобы корректно добавлялись данные, нужно следовать этим дейсвтиям:" + "\n" +
            "1) Чтобы начать работу с новой партией, нажмите в меню кнопку /addnewfabric" + "\n" +
            "2) После этого начинаете делать новый рулон, нажмите в меню кнопку /addnewroll" + "\n" +
            "3) Когда закончили рулон, нажмите в меню кнопку /finishroll" + "\n" +
            "Тоесть каждый раз когда вы начинаете делать новый рулон, вы должны нажимать кнопку /addnewroll, а когда закничваете, жмете /finishroll " +
            "Когда вы сделали нужное количесво рулонов для партии и внесли все данные в порядке, котрый указан выше, теперь вы можете нажать кнопку /finishfabric, для завершения партии." + "\n" +
            "4) Теперь вы можете завершить партию, для этого нажмите /finishfabric" + "\n" +
            "Кнопка /remarkroll для внесения замечаний по рулону, внести замечания вы можете в любой момент (например хвост или какойто косяк в рулоне)." + "\n" +
            "Кнопка /updatemetricroll для изменения метража рулона, например отрезали от рулона кусок на продажу." + "\n" +
            "Кнопка /deleteroll удаляет рулон, возможно вы могли по ошибке внести не правильные данные." + "\n" +
            "Кнопка /deletefabric удаляет партию, возможно вы могли по ошибке внести не правильные данные." + "\n" +
            "Кнопки /soldoutfabric и /soldoutroll для того чтобы пометить партию или рулон как проданные." + "\n" +
            "Остальные кнопки для вывода информации по партиям и рулонам, вся информация пишется в excel файл с 2мя листами, на первом листе данные партии, на втором листе данные по рулонам этой партии." + "\n" +
            "Чтобы выполнить какое-то действие, каждый раз вы должны выбирать в меню нужное.";

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Вступительное сообщение"));
       // listOfCommands.add(new BotCommand("/startworkshift", "Начать новую смену"));
        listOfCommands.add(new BotCommand("/addnewfabric", "Начать новую партию"));
        listOfCommands.add(new BotCommand("/addnewroll", "Начать новый рулон"));
        listOfCommands.add(new BotCommand("/finishroll", "Закончить рулон"));
        listOfCommands.add(new BotCommand("/finishfabric", "Закончить партию"));
        listOfCommands.add(new BotCommand("/remarkroll", "Внести замечания по рулону"));
        listOfCommands.add(new BotCommand("/updatemetricroll", "Обновить метраж рулона"));
        listOfCommands.add(new BotCommand("/deleteroll", "Удалить рулон"));
        listOfCommands.add(new BotCommand("/deletefabric", "Удалить партию"));
        listOfCommands.add(new BotCommand("/soldoutfabric", "Пометить партию, как проданную"));
        listOfCommands.add(new BotCommand("/soldoutroll", "Пометить рулон, как проданный"));
        listOfCommands.add(new BotCommand("/remainder", "Остаток по ткани"));
        listOfCommands.add(new BotCommand("/datareadyfabric", "Посмотреть готовые партии"));
        listOfCommands.add(new BotCommand("/datasoldoutfabric", "Посмотреть проданные партии"));
        listOfCommands.add(new BotCommand("/datasoldoutrollinreadyfabrics", "Посмотреть проданные рулоны в готовых партиях"));
        listOfCommands.add(new BotCommand("/help", "Информация по боту"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }catch (TelegramApiException e){
            e.printStackTrace();
            //   log.error("Error setting bot command list " + e.getMessage());
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

            String state = getState(chatId);

            if(messageText.equals("/start")){
                sendMessage(chatId, update.getMessage().getChat().getFirstName() + START_OUTPUT);
                saveColleague(update.getMessage());
            }

            //--------Работа с партией-------------
            if(messageText.equals("/addnewfabric")){
                //questionStatusFabric(chatId);
                sendMessage(chatId, "Для добавления новой партии введите название ткани, номер партии, метраж партии, одним предложением через пробел, в формате: А60 23.023 400");
                setState(chatId, "ADD_NEW_BATCH");
            }
            if(state.equals("ADD_NEW_BATCH")){
                saveNewFabric(chatId, messageText);
                deleteState();
            }
            if(messageText.equals("/deletefabric")){
                sendMessage(chatId, "Для удаления партии ведите номер партии, в формате: 23.023");
                setState(chatId, "DELETE_FABRIC");
            }
            if(state.equals("DELETE_FABRIC")){
                deleteFabric(chatId, messageText);
                deleteState();
            }
            if(messageText.equals("/finishfabric")){
                sendMessage(chatId, "Чтобы законить партию введите номер партии, в формате: 23.023");
                setState(chatId, "READY_FABRIC");
            }
            if(state.equals("READY_FABRIC")){
                finishTheFabric(chatId, messageText);
                deleteState();
            }
            if(messageText.equals("/soldoutfabric")){
                sendMessage(chatId, "Чтобы пометить партию как проданную, введите номер партии, в формате: 23.023");
                setState(chatId, "MARK_SOLD_OUT_FABRIC");
            }
            if(state.equals("MARK_SOLD_OUT_FABRIC")){
                updateStatusSoldOut(chatId, messageText);
                deleteState();
            }

            //--------Работа с рулоном-------------
            if(messageText.equals("/addnewroll")){
                sendMessage(chatId, "Для добавления рулона введите номер парти, номер рулона, одним предложением через пробел, в формате: 23.023 1");
                setState(chatId, "ADD_NEW_ROLL");
            }
            if(state.equals("ADD_NEW_ROLL")){
                saveNewRoll(messageText,chatId);
                deleteState();
            }
            if(messageText.equals("/remarkroll")){
                sendMessage(chatId, "Для внесения замечания по рулону введите номер партии, номер рулона и текст замечания, одним предложением через пробел, в формате: 23.023 1 текст замечания");
                setState(chatId, "REMARK_ROLL");
            }
            if(state.equals("REMARK_ROLL")){
                updateRemarkRoll(chatId, messageText);
                deleteState();
            }
            if(messageText.equals("/updatemetricroll")){
                sendMessage(chatId, "Для обновления метража рулона введите номер партии, номер рулона, сколько метров орезали от рулона, одним предложением через пробел, в формате: 23.023 1 20.2 (или 20)");
                setState(chatId, "UPDATE_METRIC_ROLL");
            }
            if(state.equals("UPDATE_METRIC_ROLL")){
                updateMetricRoll(chatId, messageText);
                deleteState();
            }
            if(messageText.equals("/finishroll")){
                sendMessage(chatId, "Чтобы закончить рулон введите номер партии, номер рулона, количество шагов, одним предложеием через пробел, в формате: 23.023 1 2500");
                setState(chatId, "FINISH_ROLL");
            }
            if(state.equals("FINISH_ROLL")){
                finishRoll(chatId, messageText);
                deleteState();
            }
            if(messageText.equals("/deleteroll")){
                sendMessage(chatId, "Чтобы удалить рулон введите номер партии, номер рулона, одним предложение через пробел, в формате: 23.023 1");
                setState(chatId, "DELETE_ROLL");
            }
            if(state.equals("DELETE_ROLL")){
                deleteRoll(chatId, messageText);
                deleteState();
            }
            if(messageText.equals("/soldoutroll")){
                sendMessage(chatId, "Чтобы пометить рулон как проданный, введите номер партии, номер рулона, в формате: 23.023 1");
                setState(chatId, "MARK_SOLD_OUT_ROLL");
            }
            if(state.equals("MARK_SOLD_OUT_ROLL")){
                updateStatusSoldOut(chatId, messageText);
                deleteState();
            }

            //--------Вывод остатков-------------
            if(messageText.equals("/remainder")){
                SendMessage sendMessage = sendMessageFromFabricKeyboardNameFabric(new SendMessage(String.valueOf(chatId), "Выберите по какой ткани показать остаток"));
                executeMessage(sendMessage);
            }
            if(getReplacedNameFabric(messageText).equals("А40")){
                writeFile.writeExcelFileReadyFabric(getRemainderFabrics(chatId, messageText), messageText);
                sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
            }
            if(getReplacedNameFabric(messageText).equals("А60")){
                writeFile.writeExcelFileReadyFabric(getRemainderFabrics(chatId, messageText), messageText);
                sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
            }
            if(getReplacedNameFabric(messageText).equals("А80")){
                writeFile.writeExcelFileReadyFabric(getRemainderFabrics(chatId, messageText), messageText);
                sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
            }
            if(getReplacedNameFabric(messageText).equals("А120")){
                writeFile.writeExcelFileReadyFabric(getRemainderFabrics(chatId, messageText), messageText);
                sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
            }
            if(getReplacedNameFabric(messageText).equals("А160")){
                writeFile.writeExcelFileReadyFabric(getRemainderFabrics(chatId, messageText), messageText);
                sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
            }

            //--------Вывод партий-------------
            if(messageText.equals("/datareadyfabric")){
                SendMessage sendMessage = sendMessageFromFabricKeyboardNumberFabric(new SendMessage(String.valueOf(chatId), "Выберите по какой партии показать данные"));
                executeMessage(sendMessage);
            }
            if(messageText.matches(REGEX_READY_FABRIC_DATA)){
                writeFile.writeExcelFileReadyFabric(getOneReadyFabric(chatId, messageText), messageText);
                sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
            }if(messageText.equals("Все готовые партии")){
                writeFile.writeExcelFileReadyFabric(getAllFabricByStatusFabric(chatId, StatusFabric.READY), messageText);
                sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
            }

            //--------Проданные партии-------------
            if(messageText.equals("/datasoldoutfabric")){
                SendMessage sendMessage = sendMessageFromFabricKeyboardPeriodDate(new SendMessage(String.valueOf(chatId), "Выберите за какой период показать проданные партии"));
                executeMessage(sendMessage);
                setState(chatId, "DATA_SOLD_OUT_FABRIC");
            }
            if(state.equals("DATA_SOLD_OUT_FABRIC")){
                Map<List<Object[]>, List<Roll>> fabricAndRollSoldOut = getAllFabricSoldOut(chatId, messageText);
                if(!fabricAndRollSoldOut.isEmpty()){
                    writeFile.writeExcelFileReadyFabric(fabricAndRollSoldOut, messageText);
                    sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
                }else {
                    sendMessage(chatId, "На выбранный период времени нет проданных партий");
                }
                deleteState();
            }

            //--------Проданные рулоны в готовых партиях-------------
            if(messageText.equals("/datasoldoutrollinreadyfabrics")){
                SendMessage sendMessage = sendMessageFromFabricKeyboardPeriodDate(new SendMessage(String.valueOf(chatId), "Выберите за какой период показать проданные рулоны"));
                executeMessage(sendMessage);
                setState(chatId, "DATA_SOLD_OUT_ROLL_IN_READY_FABRICS");
            }
            if(state.equals("DATA_SOLD_OUT_ROLL_IN_READY_FABRICS")){
                Map<List<Object[]>, List<Roll>> fabricReadyAndRollSoldOut = getAllSoldOutRollInReadyFabrics(chatId, messageText);
                if(!fabricReadyAndRollSoldOut.isEmpty()){
                    writeFile.writeExcelFileReadyFabric(fabricReadyAndRollSoldOut, messageText);
                    sendDocument(chatId, "C:/doc/" + messageText + ".xlsx");
                }else {
                    sendMessage(chatId, "На выбранный период времени нет проданных партий");
                }
                deleteState();
            }

           /* if (messageText.equals("/startworkshift")){
                sendMessage(chatId, "Чтобы начать смену введите номер партии, номер руллона одним предложение через пробел, в формате: 23.023 1");
                setState(chatId, "START_WORK_SHIFT");
            }
            if(state.equals("START_WORK_SHIFT")){
                startWorkShift(chatId, messageText);
                deleteState();
            }*/

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

    private void setState(long chatId, String state){
        userStates.put(chatId, state);
    }

    private String getState(long chatId){
        return userStates.getOrDefault(chatId, "/start");
    }

    private void deleteState(){
        userStates.clear();
    }


    //--------------COLLEAGUE-----------------------------COLLEAGUE-----------
    private void saveColleague(Message message){
        if(colleagueRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();
            Colleague colleague = new Colleague();
            colleague.setChatId(chatId);
            colleague.setFirstName(chat.getFirstName());
            colleague.setLastName(chat.getLastName());
            colleague.setUserName(chat.getUserName());
            colleague.setDateRegister(LocalDate.now());
            colleagueRepository.save(colleague);
            System.out.println("Colleague save " + colleague);
            //    log.info("Colleague save " + colleague);
        }
    }


    //--------------FABRIC----------------------------------FABRIC----------
    private void saveNewFabric(long chatId, String messageText){
        if(messageText.matches(REGEX_ADD_NEW_FABRIC)){
            String[] data = messageText.split("\s");
            if(fabricRepository.findById(data[1]).isEmpty()){
                Colleague colleague = colleagueRepository.findById(chatId).get();
                Fabric fabric = new Fabric();
                fabric.setBatchNumberId(data[1]);
                fabric.setDateManufacture(LocalDate.now());
                fabric.setMetricAreaBatch(Integer.parseInt(data[2]));
                fabric.setNameFabric(getReplacedNameFabric(data[0]));
                fabric.addColleague(colleague);
                fabric.setStatusFabric(StatusFabric.AT_WORK);
                fabricRepository.save(fabric);
                sendMessage(chatId, "Вы начали делать новую партию " + messageText);
                //     log.info(colleague.getFirstName() + " начал делать новую партию " + messageText);
            }else{
                sendMessage(chatId, "Такая партия уже сущесвует");
            }
        }else{
            sendMessage(chatId, "Не удается сохранить новую партию, неверный формат ввода");
        }
    }

    private void finishTheFabric(long chatId, String messageText){
        if(messageText.matches(REGEX_NUMBER_FABRIC)){
            Fabric fabric = fabricRepository.findById(messageText).orElse(null);
            if(fabric != null){
                fabric.setStatusFabric(StatusFabric.READY);
                fabric.setDateManufacture(LocalDate.now());
                fabricRepository.save(fabric);
                sendMessage(chatId, "Партия " + messageText + " завершена");
                //   log.info("Партия " + messageText + " завершена");
            }else{
                sendMessage(chatId, "Партии " + messageText + " не существует");
            }
        }else{
            sendMessage(chatId, "Не удается закончить партию, неверный формат ввода");
        }
    }

    @Transactional
    private void deleteFabric(long chatId, String messageText){
        if(messageText.matches(REGEX_NUMBER_FABRIC)){
            Fabric fabric = fabricRepository.findById(messageText).orElse(null);
            if (fabric != null){
                rollRepository.deleteAllByFabricId(fabric.getBatchNumberId());
                fabric.remove();
                fabricRepository.deleteFabric(fabric.getBatchNumberId());
                sendMessage(chatId, "Партия " + messageText + " была удалена");
                //   log.info("Партия " + messageText + " была удалена");
            }
            else {
                sendMessage(chatId, "Партии " + messageText + " не существует");
            }
        }else {
            sendMessage(chatId, "Не удается удалить партию, неверный формат ввода");
        }
    }

    private void updateStatusSoldOut(long chatId, String messageText){
        Fabric fabric = null;
        if(messageText.matches(REGEX_NUMBER_FABRIC)){
            fabric = fabricRepository.findById(messageText).orElse(null);
            if(fabric != null){
                fabricRepository.updateStatusFabricSoldOut(StatusFabric.SOLD_OUT.toString(), fabric.getBatchNumberId(), LocalDate.now());
                rollRepository.updateAllStatusRollSoldOut(StatusRoll.SOLD_OUT.toString(), fabric.getBatchNumberId(), LocalDate.now());
                sendMessage(chatId, "Партия " + fabric.getBatchNumberId() + " была продана");
                //   log.info("Партия " + fabric.getBatchNumberId() + " была продана");
            }else {
                sendMessage(chatId, "Такой партии не существует");
            }
        }else if(messageText.matches(REGEX_NUMBER_FABRIC_AND_ROLL)){
            String[] data = messageText.split("\s");
            int numberRoll = Integer.parseInt(data[1]);
            fabric = fabricRepository.findById(data[0]).orElse(null);
            if(fabric != null && rollRepository.findByNumberRollAndFabricId(numberRoll, data[0]).isPresent()){
                rollRepository.updateByStatusRoll(StatusRoll.SOLD_OUT.toString(), fabric.getBatchNumberId(), numberRoll, LocalDate.now());
                sendMessage(chatId, "В партии " + fabric.getBatchNumberId() + " рулон под номером " + numberRoll + " был продан");
                if(fabricRepository.sumReadyRollNull(fabric.getBatchNumberId()) == 0){
                    fabricRepository.updateStatusFabricSoldOut(StatusFabric.SOLD_OUT.toString(), fabric.getBatchNumberId(), LocalDate.now());
                }
                //    log.info("В партии " + fabric.getBatchNumberId() + " рулон под номером " + numberRoll + " был продан");
            }else {
                sendMessage(chatId, "Не существует такой партии или рулона");
            }
        }else{
            sendMessage(chatId, "Не удается обновить статус на продано, неверный формат ввода");
        }
    }

    private Map<List<Object[]>, List<Roll>> getOneReadyFabric(long chatId, String messageText){
        String[] data = messageText.split("\s");
        Map<List<Object[]>, List<Roll>> allDataFabric = new LinkedHashMap<>();
        List<Object[]> infoFabrics = fabricRepository.allInfoFabricAndSumMetricIfStatusRollReady(data[0]);
        List<Roll> rollLists = rollRepository.findByFabricId(data[0]);
        if(infoFabrics.get(0) != null && !rollLists.isEmpty()){
            allDataFabric.put(infoFabrics, rollLists);
        }else {
            sendMessage(chatId, "Партия " + data[0] + " находится в работе, по ней нет готовых рулонов");
        }
        return allDataFabric;
    }

    private Map<List<Object[]>, List<Roll>> getAllFabricByStatusFabric(long chatId, StatusFabric statusFabric) {
        Map<List<Object[]>, List<Roll>> allDataFabric = new LinkedHashMap<>();
        List<Fabric> fabrics = fabricRepository.findByAllStatusFabric(statusFabric.toString());
        for (Fabric fabric : fabrics) {
            List<Object[]> infoFabrics = fabricRepository.allInfoFabricAndSumMetricIfStatusRollReady(fabric.getBatchNumberId());
            List<Roll> rollLists = rollRepository.findByFabricId(fabric.getBatchNumberId());
            if(infoFabrics.get(0) != null && !rollLists.isEmpty()){
                allDataFabric.put(infoFabrics, rollLists);
            }else {
                sendMessage(chatId, "Партия " + fabric.getBatchNumberId() + " находится в работе, по ней нет готовых рулонов");
            }
        }
        return allDataFabric;
    }

    private Map<List<Object[]>, List<Roll>> getRemainderFabrics(long chatId, String messageText){
        Map<List<Object[]>, List<Roll>> allDataFabric = new LinkedHashMap<>();
        List<Fabric> fabricList = fabricRepository.findByNameFabric(getReplacedNameFabric(messageText));
        if(fabricList.isEmpty()){
            sendMessage(chatId, "Остатков по ткани " + messageText + " на складе нет");
        }else {
            for(Fabric fabric : fabricList){
                if(fabric.getStatusFabric().equals(StatusFabric.SOLD_OUT)){
                    continue;
                }
                List<Object[]> allInfo = fabricRepository.allInfoFabricAndSumMetricIfStatusRollReady(fabric.getBatchNumberId());
                List<Roll> allRoll = rollRepository.findByFabricId(fabric.getBatchNumberId());
                if(allInfo.get(0) != null && !allRoll.isEmpty()){
                    allDataFabric.put(allInfo, allRoll);
                }else {
                    sendMessage(chatId, "Партия " + fabric.getBatchNumberId() + " находится в работе, по ней нет готовых рулонов");
                }
            }
        }
        return allDataFabric;
    }

    private Map<List<Object[]>, List<Roll>> getAllFabricSoldOut(long chatId, String messageText){
        Set<Fabric> fabricSet = null;
        String statusFabricSoldOut = StatusFabric.SOLD_OUT.toString();
        Map<List<Object[]>, List<Roll>> allDataFabric = new LinkedHashMap<>();
        if(messageText.equals(periodDate[0])){
            fabricSet = fabricRepository.findAllByStatusSoldOutCurrentMonth(statusFabricSoldOut);
        }else if(messageText.equals(periodDate[1])){
            fabricSet = fabricRepository.findByAllByStatusSoldOutLastCountMonths(statusFabricSoldOut, 2);
        }else if(messageText.equals(periodDate[2])){
            fabricSet = fabricRepository.findByAllByStatusSoldOutLastCountMonths(statusFabricSoldOut,6);
        }else if(messageText.equals(periodDate[3])){
            fabricSet = fabricRepository.findByAllByStatusSoldOutInYear(statusFabricSoldOut);
        }
        if(fabricSet != null && !fabricSet.isEmpty()){
            for (Fabric fabric : fabricSet) {
                List<Object[]> infoFabrics = fabricRepository.allInfoFabricAndSumMetricSoldOutFabric(fabric.getBatchNumberId());
                List<Roll> rollLists = rollRepository.findByFabricIdAndStatusRollSoldOut(fabric.getBatchNumberId(), StatusRoll.SOLD_OUT.toString());
                if (infoFabrics.get(0) != null && !rollLists.isEmpty()) {
                    allDataFabric.put(infoFabrics, rollLists);
                } else {
                    sendMessage(chatId, "Партия " + fabric.getBatchNumberId() + " находится в работе, по ней нет готовых рулонов");
                }
            }
        }
        return allDataFabric;
    }

    private Map<List<Object[]>, List<Roll>> getAllSoldOutRollInReadyFabrics(long chatId, String messageText){
        Set<Fabric> fabricSet = null;
        Map<List<Object[]>, List<Roll>> allDataFabric = new LinkedHashMap<>();
        if(messageText.equals(periodDate[0])){
            fabricSet = fabricRepository.findAllByStatusSoldOutRollInReadyFabricsCurrentMonth();
        }else if(messageText.equals(periodDate[1])){
            fabricSet = fabricRepository.findAllByStatusSoldOutRollInReadyFabricsLastCountMonths(2);
        }else if(messageText.equals(periodDate[2])){
            fabricSet = fabricRepository.findAllByStatusSoldOutRollInReadyFabricsLastCountMonths(6);
        }else if(messageText.equals(periodDate[3])){
            fabricSet = fabricRepository.findAllByStatusSoldOutRollInReadyFabricsInYear();
        }
        if(fabricSet != null && !fabricSet.isEmpty()){
            for (Fabric fabric : fabricSet) {
                List<Object[]> infoFabrics = fabricRepository.allInfoFabricAndSumMetricIfStatusRollReady(fabric.getBatchNumberId());
                List<Roll> rollLists = rollRepository.findByFabricIdAndStatusRollSoldOut(fabric.getBatchNumberId(), StatusRoll.SOLD_OUT.toString());
                if (infoFabrics.get(0) != null && !rollLists.isEmpty()) {
                    allDataFabric.put(infoFabrics, rollLists);
                } else {
                    sendMessage(chatId, "Партия " + fabric.getBatchNumberId() + " находится в работе, по ней нет готовых рулонов");
                }
            }
        }
        return allDataFabric;
    }
/*
    private void startWorkShift(long chatId, String messageText){
        String[] data = messageText.split("\s");
        Fabric fabric = fabricRepository.findById(data[0]).orElse(null);
        Colleague colleague = colleagueRepository.findById(chatId).orElse(null);
        if(fabric != null){
            Roll roll = rollRepository.findByNumberRollAndFabricId(Integer.parseInt(data[1]), fabric.getBatchNumberId()).orElse(null);
            if(roll == null){
                Roll newRoll = new Roll();
                newRoll.setNumberRoll(Integer.parseInt(data[1]));
                newRoll.setRollMetric(0);
                newRoll.setDateFulfilment(LocalDateTime.now());
                newRoll.setStatusRoll(StatusRoll.AT_WORK);
                newRoll.setRemark(null);
                newRoll.setFabric(fabric);
                newRoll.addColleague(colleague);
                rollRepository.save(newRoll);
                sendMessage(chatId, colleague.getFirstName() + " начал делать новый рулон под номером " + data[1] + " для партии " + data[0]);
            }else{
                roll.addColleague(colleague);
                sendMessage(chatId, "Вы продолжаете делать рулон");
            }
        }else {
            sendMessage(chatId, "Для добавления новой партии нажмите кнопку в меню /addnewbatch");
        }
    }*/

    //--------------ROLL------------------------------------ROLL----------
    private void saveNewRoll(String messageText, long chatId){
        if(messageText.matches(REGEX_NUMBER_FABRIC_AND_ROLL)){
            String[] data = messageText.split("\s");
            Fabric fabric = fabricRepository.findById(data[0]).orElse(null);
            if(fabric != null && rollRepository.findByNumberRollAndFabricId(Integer.parseInt(data[1]), data[0]).isEmpty()){
                Roll roll = new Roll();
                roll.setNumberRoll(Integer.parseInt(data[1]));
                roll.setRollMetric(0.0);
                roll.setDateFulfilment(LocalDate.now());
                roll.setStatusRoll(StatusRoll.AT_WORK);
                roll.setRemark(null);
                roll.setFabric(fabric);
                rollRepository.save(roll);
                sendMessage(chatId, "Вы начали делать " + data[1] + "й рулон, партии " + data[0]);
                //    log.info("Вы начали делать " + data[1] + "й рулон, партии " + data[0]);
            }else {
                sendMessage(chatId, "Не удается добавить рулон, вы ввели несуществующий номер партии или рулон под номером "+ data[1] + " уже сущесвует");
            }
        }else{
            sendMessage(chatId, "Не удается сохранить новый руллон, неверный формат ввода");
        }
    }

    private void updateRemarkRoll(long chatId, String messageText){
        if(messageText.matches(REGEX_REMARK_ROLL)){
            String[] data = messageText.split("\s");
            String numberFabricAndRoll = data[0] + " " + data[1] + " ";
            String remark = messageText.replaceFirst(numberFabricAndRoll, "");
            Fabric fabric = fabricRepository.findById(data[0]).orElse(null);
            if(fabric != null){
                Roll roll = rollRepository.findByNumberRollAndFabricId(Integer.parseInt(data[1]), data[0]).get();
                roll.setRemark(remark);
                roll.setDateFulfilment(LocalDate.now());
                rollRepository.save(roll);
                sendMessage(chatId, "Вы внесли замечание по рулону " + data[1] + " в партии " + data[0]);
                //   log.info("Замечание по рулону " + data[1] + " " + remark + " в партии " + data[0]);
            }else{
                sendMessage(chatId, "Партии с номером " + data[0] + " не существует");
            }
        }else {
            sendMessage(chatId, "Не удается внести замечания по рулону, неверный формат ввода");
        }
    }

    private void updateMetricRoll(long chatId, String messageText){
        String text = messageText.replaceAll(",", ".");
        if(text.matches(REGEX_UPDATE_METRIC_ROLL) || text.matches(REGEX_NUMBER_FABRIC_AND_ROLL_AND_METRIC)){
            String[] data = text.split("\s");
            double metricPiece = Double.parseDouble(data[2]);
            Roll roll = rollRepository.findByNumberRollAndFabricId(Integer.parseInt(data[1]), data[0]).orElse(null);
            if(roll != null && fabricRepository.findById(data[0]).isPresent() && roll.getRollMetric() >= metricPiece){
                double metric = roll.getRollMetric() - metricPiece;
                double roundedMetric = Math.round(metric * 10.0) / 10.0;
                roll.setRollMetric(roundedMetric);
                rollRepository.save(roll);
                sendMessage(chatId, "Вы отрезали " + metricPiece + " метров" + " от рулона " + data[1]  + " , остаток составляет " + roll.getRollMetric());
                //    log.info("Партия "+ roll.getFabric().getBatchNumberId() + ", вы отрезали " + metricPiece + " метров" + " от рулона " + data[1]  + ", остаток составляет " + roll.getRollMetric() + " метров");
            }else {
                sendMessage(chatId, "В базе данных нет такой партии или руллона, либо метраж руллона меньше, чем размер который хотите отрезать");
            }
        }else {
            sendMessage(chatId, "Не удается обновить метраж руллона, неверный формат ввода");
        }
    }

    private void finishRoll(long chatId, String messageText){
        if (messageText.matches(REGEX_NUMBER_FABRIC_AND_ROLL_AND_STEP)) {
            String[] data = messageText.split("\s");
            Roll roll = rollRepository.findByNumberRollAndFabricId(Integer.parseInt(data[1]), data[0]).orElse(null);
            if(roll != null){
                String nameFabric = roll.getFabric().getNameFabric();
                double rollMetric = convertStepInMetric(nameFabric, Integer.parseInt(data[2]));
                roll.setStatusRoll(StatusRoll.READY);
                roll.setRollMetric(rollMetric);
                roll.setDateFulfilment(LocalDate.now());
                rollRepository.save(roll);
                sendMessage(chatId, "Вы закончили " + data[1] + "й рулон, партии " + data[0]);
                //     log.info("Вы закончили " + data[1] + "й рулон, партии " + data[0]);
            }else{
                sendMessage(chatId, "Рулона " + data[1] + " не существует");
            }
        }else {
            sendMessage(chatId, "Не удается закончить рулон, неверный формат ввода");
        }
    }

    private double convertStepInMetric(String nameFabric, int steps){
        double rollMetric = 0;
        if(nameFabric.equals("А40")){
            rollMetric = Math.round(steps * widthsThread[0] * widthRoll * 10.0) / 10.0;
        } else if(nameFabric.equals("А60")){
            rollMetric = Math.round(steps * widthsThread[2] * widthRoll * 10.0) / 10.0;
        } else if(nameFabric.equals("А80")){
            rollMetric = Math.round(steps * widthsThread[0] * widthRoll * 10.0) / 10.0;
        } else if(nameFabric.equals("А120")){
            rollMetric = Math.round(steps * widthsThread[2] * widthRoll * 10.0) / 10.0;
        } else if(nameFabric.equals("А160")){
            rollMetric = Math.round(steps * widthsThread[1] * widthRoll * 10.0) / 10.0;
        }
        return rollMetric;
    }

    @Transactional
    private void deleteRoll(long chatId, String messageText){
        if(messageText.matches(REGEX_NUMBER_FABRIC_AND_ROLL)){
            String[] data = messageText.split("\s");
            Roll roll = rollRepository.findByNumberRollAndFabricId(Integer.parseInt(data[1]), data[0]).orElse(null);
            if(roll != null){
                /*roll.remove();
                rollRepository.deleteFromRollAndColleague(roll.getId());*/
                rollRepository.deleteByFabricIdAndNumberRoll(data[0], Integer.parseInt(data[1]));
                sendMessage(chatId, "Вы удалили " + data[1] + "й рулон ,партии " + data[0]);
                //     log.info("Вы удалили " + data[1] + "й рулон ,партии " + data[0]);
            }else{
                sendMessage(chatId, "Рулона " + data[1] + " не существует");
            }
        }else{
            sendMessage(chatId, "Не удается удалить рулон, неверный формат ввода");
        }
    }


    //--------------SEND_MESSAGE-----------------------------SEND_MESSAGE---------
    private void sendMessage(long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        executeMessage(sendMessage);
    }

    private void sendDocument(Long chatId, String filePath) {
        File file = new File(filePath);
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(String.valueOf(chatId));
        sendDocument.setDocument(new InputFile(file));
        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            //    log.error("ERROR_TEXT " + e.getMessage());
        }
    }

    private SendMessage sendMessageFromFabricKeyboardNumberFabric(SendMessage sendMessage){
        List<Fabric> fabricList = fabricRepository.findByAllStatusFabric(StatusFabric.READY.toString());
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        if(fabricList.size() >= 5) {
            for (int i = 0; i < fabricList.size(); i++) {
                row.add(fabricList.get(i).getBatchNumberId() + " " + fabricList.get(i).getNameFabric());
                if ((i + 1) % 5 == 0) {
                    keyboardRows.add(row);
                    row = new KeyboardRow();
                }
            }
        }else {
           for (int i = 0; i < fabricList.size(); i++){
               row.add(fabricList.get(i).getBatchNumberId() + " " + fabricList.get(i).getNameFabric());
           }
        }
        row.add("Все готовые партии");
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        keyboardRows.add(row);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        replyKeyboardMarkup.setResizeKeyboard(true);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        return sendMessage;
    }

    public SendMessage sendMessageFromFabricKeyboardNameFabric(SendMessage sendMessage){
        Set<String> setName = fabricRepository.findAllNameFabricAndStatusSoldOut(StatusFabric.SOLD_OUT.toString());
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        if(setName.size() >= 5) {
            int count = 0;
            for(String name : setName){
                row.add(name);
                if ((count + 1) % 5 == 0) {
                    keyboardRows.add(row);
                    row = new KeyboardRow();
                }
                count++;
            }
        }else {
            for(String name : setName){
                row.add(name);
            }
        }
        keyboardRows.add(row);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        replyKeyboardMarkup.setResizeKeyboard(true);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        return sendMessage;
    }

    public SendMessage sendMessageFromFabricKeyboardPeriodDate(SendMessage sendMessage){
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        for(String s : periodDate){
            row.add(s);
        }
        keyboardRows.add(row);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        return sendMessage;
    }


    //--------------EXECUTE_MESSAGE-----------------------EXECUTE_MESSAGE------------
    private void executeEditMessageText(String text, long chatId, long messageId){
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            //     log.error("ERROR_TEXT " + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message){
        try {
            execute(message);
        } catch (TelegramApiException e) {
            //    log.error("ERROR_TEXT " + e.getMessage());
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

    private String getReplacedNameFabric(String messageText){
        String nameFabric = "";
        if(messageText.contains("A")){
            nameFabric = messageText.replaceFirst("A", "А");
            return nameFabric;
        }
        return messageText;
    }

}
