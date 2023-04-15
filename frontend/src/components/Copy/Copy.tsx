import React, { PureComponent } from 'react';
import { Tooltip } from '@mantine/core';
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore for tree shaking purposes
import IconCopy from '@tabler/icons-react/dist/esm/icons/IconCopy';
import copyToClipboard from 'src/infrastructure/helpers/copy-to-clipboard';
import styles from './Copy.css';

type State = {
  tooltipVisible: boolean;
  message?: string | null;
};

type Props = {
  targetValue: string;
};

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
    const content = window.getSelection();
    if (!content) return;
    const selection = content.toString();
    if (selection.length > 0) return;
    if (this.timer) clearTimeout(this.timer);
    const last =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;
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
      <button
        type="button"
        className={styles.copyButton}
        onClick={this.handleCopy}
        onKeyDown={this.handleCopyKeyboard}
      >
        <Tooltip label={this.state.message} opened={this.state.tooltipVisible}>
          <IconCopy color="grey" size="1.2rem" />
        </Tooltip>
      </button>
    );
  }
}
