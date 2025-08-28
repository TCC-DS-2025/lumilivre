package br.com.lumilivre.api.utils;

public class CpfValidator {
    public static boolean isCpfValido(String cpf) {
        if (cpf == null || cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            int soma = 0;
            for (int i = 0; i < 9; i++) {
                soma += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
            }
            int primeiroDigito = 11 - (soma % 11);
            if (primeiroDigito > 9)
                primeiroDigito = 0;

            if (Character.getNumericValue(cpf.charAt(9)) != primeiroDigito)
                return false;

            soma = 0;
            for (int i = 0; i < 10; i++) {
                soma += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
            }
            int segundoDigito = 11 - (soma % 11);
            if (segundoDigito > 9)
                segundoDigito = 0;

            return Character.getNumericValue(cpf.charAt(10)) == segundoDigito;
        } catch (Exception e) {
            return false;
        }
    }
}
