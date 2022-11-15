import React, { PureComponent } from 'react';
import CopyIcon from '@platform-ui/iconsPack/interface/24/Copy';
import Tooltip from '@platform-ui/tooltip';
import { PortalWrapper } from '@platform-ui/portal';
import copyToClipboard from 'src/infrastructure/helpers/copy-to-clipboard';
import styles from './Copy.css';

interface State {
  tooltipVisible: boolean;
  message?: string | null;
}

interface Props {
  targetValue: string;
}

const HIDE_AFTER = 1000;
const ENTER = 13;
const SPACE = 32;

export default class Copy extends PureComponent<Props, State> {
  state: State = { tooltipVisible: false };

  timer: number | null = null;

  componentWillUnmount() {
    if (this.timer) clearTimeout(this.timer);
  }

  handleCopy = () => {
    if (!this.props.targetValue) return;
    const selection = window.getSelection().toString();
    if (selection.length > 0) return;
    if (this.timer) clearTimeout(this.timer);
    const last = document.activeElement;
    copyToClipboard(this.props.targetValue, (err) => {
      this.setState({
        tooltipVisible: !err,
        message: err ? 'Не удалось скопировать' : 'Скопировано',
      });
      if (last) last.focus();
      this.timer = setTimeout(() => {
        this.timer = null;
        this.setState({
          tooltipVisible: false,
          message: null,
        });
      }, HIDE_AFTER) as any;
    });
  };

  handleCopyKeyboard = (e: React.KeyboardEvent<HTMLButtonElement>) => {
    if (e.keyCode === ENTER || e.keyCode === SPACE) {
      e.preventDefault();
      this.handleCopy();
    }
  };

  render() {
    return (
      <PortalWrapper>
        <button
          type="button"
          className={styles.copyButton}
          onClick={this.handleCopy}
          onKeyDown={this.handleCopyKeyboard}
        >
          <Tooltip
            popoverContent={this.state.message}
            direction="top"
            align="start"
            smartDirection
            theme="dark"
            isActive={this.state.tooltipVisible}
            isInactive={!this.state.tooltipVisible}
          >
            <CopyIcon theme="gray" />
          </Tooltip>
        </button>
      </PortalWrapper>
    );
  }
}
