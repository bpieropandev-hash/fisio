package com.physio.application.service;

public class ConfiguracoesVersaoConflitanteException extends RuntimeException {
    public ConfiguracoesVersaoConflitanteException() {
        super("As configurações foram alteradas por outro usuário. Recarregue antes de salvar.");
    }
}
