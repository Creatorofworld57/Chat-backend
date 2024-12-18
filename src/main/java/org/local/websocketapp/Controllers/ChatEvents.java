package org.local.websocketapp.Controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.local.websocketapp.Models.Chat;
import org.local.websocketapp.Models.Data;
import org.local.websocketapp.Models.Message;
import org.local.websocketapp.Models.UserC;
import org.local.websocketapp.Repositories.ChatRepository;
import org.local.websocketapp.Repositories.ImageRepository;
import org.local.websocketapp.Repositories.MessageRepository;
import org.local.websocketapp.Repositories.UserRepository;
import org.local.websocketapp.Services.ServiceForChat;
import org.local.websocketapp.Utils.JwtTokenUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("/apiChats")
public class ChatEvents {

    UserRepository userRepository;
    ChatRepository chatRepository;

    MessageRepository messageRepository;
    JwtTokenUtils jwtTokenUtils;


    @GetMapping("/getChats")
    public ResponseEntity<List<Chat>> getChats(HttpServletRequest request) {
        String username = jwtTokenUtils.extractUserName(request.getHeader("Authorization").substring(7));

        Optional<UserC> userOpt = userRepository.findUserCByName(username);
        String userName = userOpt.get().getName();
        List<Chat> buffer = chatRepository.findChatByUserC(userOpt.get().getChats());
        buffer.forEach(i -> {
            if (i.getParticipants().size() == 2) {
                String name = i.getName();
                name = name.replaceFirst(", " + userName, "") // Удалить ", userName"
                        .replaceFirst(userName + ", ", ""); // Удалить "userName, "
                i.setName(name.trim()); // Удалить лишние пробелы, если есть
            }
        });

        //   buffer.forEach(i -> i.setName(i.getName().replaceFirst("(,\\s*" + userName + ")|" + userName, "")));

        return userOpt.map(userC -> ResponseEntity.ok(buffer)).orElseGet(() -> ResponseEntity.ok(List.of()));
    }

    @GetMapping("/getChats/{id}")
    public ResponseEntity<Chat> getChat(@PathVariable Long id) {
        Chat chatOpt = chatRepository.findChatById(id).get();
        return ResponseEntity.ok(chatOpt);
    }

    @PostMapping("/createChat")
    public void createChat(HttpServletRequest request, @RequestBody List<Long> users) {
        //извлекаем из токена username
        String username = jwtTokenUtils.extractUserName(request.getHeader("Authorization").substring(7));


        // Получаем пользователей по их ID

        List<UserC> chatUsers = userRepository.findAllUserCWithId(users);
        //Добавляем самого юзера к остальным ЧЛЕНАМ чата
        UserC mainUser = userRepository.findUserCByName(username).get();
        chatUsers.add(mainUser);

        // Извлекаем имена участников в виде списка
        List<String> participantNames = chatUsers.stream()
                .map(UserC::getName)
                .collect(Collectors.toList());

        // Создаем новый чат и устанавливаем участников и имя
        Chat chat = new Chat();
        users.add(mainUser.getId());
        chat.setParticipants(users);
        //солздаем фото для чата
        //  chat.setImage( serviceForChat.mergeImagesService(chatUsers.stream()
        //           .map(UserC::getPreviewImageId)
        //          .collect(Collectors.toList())));
        // chat.setImage(imageRepository.findById(chatUsers.get(0).getPreviewImageId()).get().getBytes());


        // Устанавливаем имя чата как строку с именами участников, разделенными запятой
        chat.setName(String.join(", ", participantNames));
        // Сохраняем чат в репозиторий
        chatRepository.save(chat);
        //Сохраняем чат чтобы установился id и затем достаем его и используем для установки чата для каждого пользователя
        Chat chatNew = chatRepository.findChatByName(String.join(", ", participantNames)).get();
        //Устанавливаем для каждого пользователя новый чат
        chatUsers.forEach(user -> user.getChats().add(chatNew.getId()));
        userRepository.saveAll(chatUsers);
    }


    @GetMapping("/getMessages/{id}")
    @Transactional
    public ResponseEntity<List<Message>> getMessages(@PathVariable Long id) {
        if (id != null && id != 0) {
            System.out.println("getMessage");
            Chat chatOpt = chatRepository.findChatById(id).get();
            return ResponseEntity.ok(messageRepository.findMessageByChat(chatOpt));
        } else return ResponseEntity.ok(List.of());
    }

    @GetMapping("/getLastMessage/{id}")
    public ResponseEntity<String> getLastMessage(@PathVariable Long id) {
        Chat chatOpt = chatRepository.findChatById(id).get();
        Data data = new Data();
        data.setName(chatOpt.getLastMessage());

        return ResponseEntity.ok().body(data.getName());
    }

    @GetMapping("/chatImages/{id}")
    public ResponseEntity<?> likedPlaylistImages(@PathVariable Long id) {
        byte[] massa = chatRepository.findImagesById(id);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/jpeg"))
                .contentLength(massa.length)
                .body(new InputStreamResource(new ByteArrayInputStream(massa)));
    }

    @PostMapping("/chatExist")
    public ResponseEntity<?> chatExist(HttpServletRequest request, @RequestBody List<Long> users) {
        String username = jwtTokenUtils.extractUserName(request.getHeader("Authorization").substring(7));
        users.add(userRepository.findUserCByName(username).get().getId());
        List<Chat> chats = chatRepository.findChatsWithTwoParticipants();
        System.out.println(chats);
        Long id = null;
        System.out.println("USers: "+users);
        for (Chat chat : chats) {
            System.out.println(chat.getParticipants());
            if (chat.getParticipants().size() == 2 && chat.getParticipants().contains(users.get(0))&& chat.getParticipants().contains(users.get(1))) {
                id = chat.getId();
                break;
            }
        }
        System.out.println(id);
        if (id == null) {
            return ResponseEntity.status(200).build();
        } else return ResponseEntity.status(201).body(id);
    }


}
