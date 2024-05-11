package org.jianghu.app.config.jinjava;

import com.hubspot.jinjava.tree.parse.TokenScannerSymbols;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomTokenScannerSymbols extends TokenScannerSymbols {

  char TOKEN_PREFIX_CHAR = '{';
  char TOKEN_POSTFIX_CHAR = '}';
  char TOKEN_FIXED_CHAR = 0;
  char TOKEN_NOTE_CHAR = '#';
  char TOKEN_TAG_CHAR = '%';
  char TOKEN_EXPR_START_CHAR = '$';
  char TOKEN_EXPR_END_CHAR = '$';
  char TOKEN_NEWLINE_CHAR = '\n';
  char TOKEN_TRIM_CHAR = '-';

  @Override
  public char getPrefixChar() {
    return TOKEN_PREFIX_CHAR;
  }

  @Override
  public char getPostfixChar() {
    return TOKEN_POSTFIX_CHAR;
  }

  @Override
  public char getFixedChar() {
    return TOKEN_FIXED_CHAR;
  }

  @Override
  public char getNoteChar() {
    return TOKEN_NOTE_CHAR;
  }

  @Override
  public char getTagChar() {
    return TOKEN_TAG_CHAR;
  }

  @Override
  public char getExprStartChar() {
    return TOKEN_EXPR_START_CHAR;
  }

  @Override
  public char getExprEndChar() {
    return TOKEN_EXPR_END_CHAR;
  }

  @Override
  public char getNewlineChar() {
    return TOKEN_NEWLINE_CHAR;
  }

  @Override
  public char getTrimChar() {
    return TOKEN_TRIM_CHAR;
  }

}
