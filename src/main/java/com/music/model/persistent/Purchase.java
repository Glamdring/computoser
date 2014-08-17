/*
 * Computoser is a music-composition algorithm and a website to present the results
 * Copyright (C) 2012-2014  Bozhidar Bozhanov
 *
 * Computoser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Computoser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Computoser.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.music.model.persistent;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

@Entity
@Deprecated
public class Purchase {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "INT(11) UNSIGNED")
    private Long id;

    @ManyToMany(fetch=FetchType.EAGER)
    @JoinTable(name = "purchasePieces", joinColumns = { @JoinColumn(name = "purchaseId") }, inverseJoinColumns = { @JoinColumn(name = "pieceId") })
    private Set<Piece> pieces;

    @ManyToMany(fetch=FetchType.EAGER)
    @JoinTable(name = "purchasePacks", joinColumns = { @JoinColumn(name = "purchaseId") }, inverseJoinColumns = { @JoinColumn(name = "packId") })
    private Set<PiecePack> packs;

    private String email;

    @ManyToOne
    private User user;

    @Column
    private String paymentProviderId;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Column(nullable=false)
    private boolean complete;

    @Type(type="com.music.util.persistence.PersistentDateTime")
    private DateTime purchaseTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<Piece> getPieces() {
        return pieces;
    }

    public void setPieces(Set<Piece> pieces) {
        this.pieces = pieces;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPaymentProviderId() {
        return paymentProviderId;
    }

    public void setPaymentProviderId(String paymentProviderId) {
        this.paymentProviderId = paymentProviderId;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public DateTime getPurchaseTime() {
        return purchaseTime;
    }

    public void setPurchaseTime(DateTime purchaseTime) {
        this.purchaseTime = purchaseTime;
    }

    public Set<PiecePack> getPacks() {
        return packs;
    }

    public void setPacks(Set<PiecePack> packs) {
        this.packs = packs;
    }
}
