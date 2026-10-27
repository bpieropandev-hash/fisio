package com.physio.application.service;
public class EvolucaoVersaoConflitanteException extends RuntimeException {
    public EvolucaoVersaoConflitanteException() { super("A evolução foi alterada por outra pessoa. Recarregue a ficha antes de salvar."); }
}
