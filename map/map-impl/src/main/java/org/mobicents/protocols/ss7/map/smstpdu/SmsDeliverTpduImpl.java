/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.protocols.ss7.map.smstpdu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
import org.mobicents.protocols.ss7.map.api.smstpdu.AddressField;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.api.smstpdu.ProtocolIdentifier;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsDeliverTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsTpduType;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserData;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmsDeliverTpduImpl extends SmsTpduImpl implements SmsDeliverTpdu {

	private boolean moreMessagesToSend;
	private boolean forwardedOrSpawned;
	private boolean replyPathExists;
	private boolean userDataHeaderIndicator;
	private boolean statusReportIndication;
	private AddressFieldImpl originatingAddress;
	private ProtocolIdentifierImpl protocolIdentifier;
	private DataCodingSchemeImpl dataCodingScheme;
	private AbsoluteTimeStampImpl serviceCentreTimeStamp;
	private int userDataLength;
	private UserDataImpl smsUserData;

	private SmsDeliverTpduImpl() {
		this.tpduType = SmsTpduType.SMS_DELIVER;
		this.mobileOriginatedMessage = false;
	}

	public SmsDeliverTpduImpl(boolean moreMessagesToSend, boolean forwardedOrSpawned, boolean replyPathExists, boolean userDataHeaderIndicator,
			boolean statusReportIndication, AddressFieldImpl originatingAddress) throws MAPException {
		this();

		this.moreMessagesToSend = moreMessagesToSend;
		this.forwardedOrSpawned = forwardedOrSpawned;
		this.replyPathExists = replyPathExists;
		this.userDataHeaderIndicator = userDataHeaderIndicator;
		this.statusReportIndication = statusReportIndication;
		this.originatingAddress = originatingAddress;
	}

	public SmsDeliverTpduImpl(byte[] data, Charset gsm8Charset) throws MAPException {
		this();

		if (data == null)
			throw new MAPException("Error creating a new SmsDeliverTpduImpl instance: data is empty");
		if (data.length < 1)
			throw new MAPException("Error creating a new SmsDeliverTpduImpl instance: data length is equal zero");

		ByteArrayInputStream stm = new ByteArrayInputStream(data);

		int bt = stm.read();
		if ((bt & _MASK_TP_MMS) == 0)
			this.moreMessagesToSend = true;
		if ((bt & _MASK_TP_LP) != 0)
			this.forwardedOrSpawned = true;
		if ((bt & _MASK_TP_RP) != 0)
			this.replyPathExists = true;
		if ((bt & _MASK_TP_UDHI) != 0)
			this.userDataHeaderIndicator = true;
		if ((bt & _MASK_TP_SRI) != 0)
			this.statusReportIndication = true;

		this.originatingAddress = AddressFieldImpl.createMessage(stm);

		bt = stm.read();
		if (bt == -1)
			throw new MAPException("Error creating a new SmsDeliverTpduImpl instance: protocolIdentifier field has not been found");
		this.protocolIdentifier = new ProtocolIdentifierImpl(bt);

		bt = stm.read();
		if (bt == -1)
			throw new MAPException("Error creating a new SmsDeliverTpduImpl instance: dataCodingScheme field has not been found");
		this.dataCodingScheme = new DataCodingSchemeImpl(bt);

		this.serviceCentreTimeStamp = AbsoluteTimeStampImpl.createMessage(stm);

		this.userDataLength = stm.read();
		if (this.userDataLength == -1)
			throw new MAPException("Error creating a new SmsDeliverTpduImpl instance: userDataLength field has not been found");

		int avail = stm.available();
		byte[] buf = new byte[avail];
		try {
			stm.read(buf);
		} catch (IOException e) {
			throw new MAPException("IOException while creating a new SmsDeliverTpduImpl instance: " + e.getMessage(), e);
		}
		smsUserData = new UserDataImpl(buf, dataCodingScheme, userDataLength, userDataHeaderIndicator, gsm8Charset);
	}

	@Override
	public boolean getMoreMessagesToSend() {
		return this.moreMessagesToSend;
	}

	@Override
	public boolean getForwardedOrSpawned() {
		return this.forwardedOrSpawned;
	}

	@Override
	public boolean getReplyPathExists() {
		return this.replyPathExists;
	}

	@Override
	public boolean getUserDataHeaderIndicator() {
		return this.userDataHeaderIndicator;
	}

	@Override
	public boolean getStatusReportIndication() {
		return this.statusReportIndication;
	}

	@Override
	public AddressField getOriginatingAddress() {
		return originatingAddress;
	}

	@Override
	public ProtocolIdentifier getProtocolIdentifier() {
		return protocolIdentifier;
	}

	@Override
	public DataCodingScheme getDataCodingScheme() {
		return dataCodingScheme;
	}

	@Override
	public AbsoluteTimeStamp getServiceCentreTimeStamp() {
		return serviceCentreTimeStamp;
	}

	@Override
	public int getUserDataLength() {
		return userDataLength;
	}

	@Override
	public UserData getUserData() {
		return smsUserData;
	}

	@Override
	public byte[] encodeData() throws MAPException {
		
		ByteArrayOutputStream res = new ByteArrayOutputStream();
		
		// byte 0
		res.write(SmsTpduType.SMS_DELIVER.getCode() | (!this.moreMessagesToSend ? _MASK_TP_MMS : 0) | (this.forwardedOrSpawned ? _MASK_TP_LP : 0)
				| (this.replyPathExists ? _MASK_TP_RP : 0) | (this.userDataHeaderIndicator ? _MASK_TP_UDHI : 0)
				| (this.statusReportIndication ? _MASK_TP_SRI : 0));
		
		// TODO: implement encoding ................................

		return res.toByteArray();
	}
}