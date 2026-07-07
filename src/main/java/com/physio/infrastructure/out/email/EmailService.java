package com.physio.infrastructure.out.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String remetente;

    public void enviarEmailRedefinicaoSenha(String destinatario, String linkRedefinicao) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject("Redefinição de senha - Physio Manager");
        mensagem.setText(
                "Recebemos uma solicitação para redefinir sua senha.\n\n" +
                "Clique no link abaixo para criar uma nova senha (válido por 30 minutos):\n" +
                linkRedefinicao + "\n\n" +
                "Se você não solicitou essa alteração, ignore este email."
        );

        try {
            mailSender.send(mensagem);
        } catch (Exception ex) {
            // Nunca propagar: o endpoint de esqueci-senha sempre responde 200,
            // independente do envio de email ter sucesso.
            log.error("Falha ao enviar email de redefinição de senha para {}", destinatario, ex);
        }
    }
}
