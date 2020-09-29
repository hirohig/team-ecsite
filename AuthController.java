package jp.co.internous.mushrooms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

import jp.co.internous.mushrooms.model.domain.MstUser;
import jp.co.internous.mushrooms.model.form.UserForm;
import jp.co.internous.mushrooms.model.mapper.MstUserMapper;
import jp.co.internous.mushrooms.model.mapper.TblCartMapper;
import jp.co.internous.mushrooms.model.session.LoginSession;

@RestController
@RequestMapping("/mushrooms/auth")
public class AuthController {
	
	private Gson gson = new Gson();
	
	@Autowired
	private MstUserMapper mstUserMapper;
	
	@Autowired
	private TblCartMapper tblCartMapper;
	
	@Autowired
	private LoginSession loginSession;
	
	@RequestMapping("/login")
	public String login(@RequestBody UserForm form) {
		
		//DBの会員情報マスタテーブルから、送られてきたユーザー名とパスワードに紐づくユーザー情報を取得する。
		MstUser user = mstUserMapper.findByUserNameAndPassword(form.getUserName(), form.getPassword());
		
		long tmpUserId = loginSession.getTmpUserId();
		
		//DBにユーザーが存在するか確認し、存在する場合は仮ユーザーIDに紐づくカート情報をユーザーIDに紐付け直す。
		if (user != null && tmpUserId != 0) {
			int count = tblCartMapper.findCountByUserId(tmpUserId);
			if (count > 0) {
				tblCartMapper.updateByUserId(user.getId(), tmpUserId);
			}
		}
		
		//DBにユーザーが存在するか確認し、成否によりログインセッションの内容を変更する。
		if (user != null) {
			loginSession.setUserId(user.getId());
			loginSession.setTmpUserId(0);
			loginSession.setUserName(user.getUserName());
			loginSession.setPassword(user.getPassword());
			loginSession.setLogined(true);
		} else {
			loginSession.setUserId(0);
			loginSession.setUserName(null);
			loginSession.setPassword(null);
			loginSession.setLogined(false);;
		}

		return gson.toJson(user);
	}
	
	@RequestMapping("/logout")
	public String logout() {
		
		loginSession.setUserId(0);
		loginSession.setTmpUserId(0);
		loginSession.setUserName(null);
		loginSession.setPassword(null);
		loginSession.setLogined(false);
		
		return "";
	}
	
	@RequestMapping("/resetPassword")
	public String resetPassword(@RequestBody UserForm f) {
		//パスワード再設定が成功した時の更新成功メッセージを予めmessageに代入しておく。
		String message = "パスワードが再設定されました。";
		
		String newPassword = f.getNewPassword();
		String newPasswordConfirm = f.getNewPasswordConfirm();
		
		MstUser user = mstUserMapper.findByUserNameAndPassword(f.getUserName(), f.getPassword());
		
		if (user == null) {
			//入力されたパスワードに紐づくユーザーがDBに存在しない場合、下記のメッセージをフロントに返す。
			message = "現在のパスワードが正しくありません。";
		} else if (user.getPassword().equals(newPassword)) {
			//現パスワードと新パスワードが一致する場合、下記のメッセージをフロントに返す。
			message = "現在の文字列と同一文字列が入力されました。";
		} else if (!(newPassword.equals(newPasswordConfirm))) {
			//新パスワードと新パスワードの確認が一致しない場合、下記のメッセージをフロントに返す。
			message = "新パスワードと確認用パスワードが一致しません。";
		} else {
			//パスワード再設定が成功した場合、以下の処理を行い、予め代入しておいた更新成功のメッセージをフロントに返す。
			//DBの会員情報マスタテーブルとセッションの現パスワードを新パスワードに更新する。
			mstUserMapper.updatePassword(user.getUserName(), newPassword);
			loginSession.setPassword(newPassword);			
		}
		
		return message;
	}
	
}
